/*
 * Copyright 2017 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.asyncenum;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

final class AsyncRetry<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final long times;

    final Predicate<? super Throwable> predicate;

    AsyncRetry(AsyncEnumerable<T> source, long times, Predicate<? super Throwable> predicate) {
        this.source = source;
        this.times = times;
        this.predicate = predicate;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new RetryEnumerator<>(source, times, predicate);
    }

    static final class RetryEnumerator<T>
            extends AtomicInteger
            implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerable<T> source;

        long times;

        final Predicate<? super Throwable> predicate;

        final AtomicReference<AsyncEnumerator<T>> current;

        T result;

        CompletableFuture<Boolean> completable;

        RetryEnumerator(AsyncEnumerable<T> source, long times, Predicate<? super Throwable> predicate) {
            this.source = source;
            this.current = new AtomicReference<>(source.enumerator());
            this.times = times;
            this.predicate = predicate;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            nextItem();
            return cf;
        }

        @Override
        public T current() {
            return result;
        }

        @Override
        public void cancel() {
            AsyncEnumeratorHelper.cancel(current);
        }

        void nextItem() {
            if (getAndIncrement() == 0) {
                do {
                    current.get().moveNext().whenComplete(this);
                } while (decrementAndGet() != 0);
            }
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                if (times-- <= 0L || !predicate.test(throwable)) {
                    result = null;
                    completable.completeExceptionally(throwable);
                } else {
                    if (AsyncEnumeratorHelper.replace(current, source.enumerator())) {
                        nextItem();
                    }
                }
                return;
            }

            if (aBoolean) {
                result = current.getPlain().current();
                completable.complete(true);
            } else {
                result = null;
                completable.complete(false);
            }
        }
    }
}

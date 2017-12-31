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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

final class AsyncLast<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    AsyncLast(AsyncEnumerable<T> source) {
        this.source = source;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new LastEnumerator<>(source.enumerator());
    }

    static final class LastEnumerator<T>
            extends AtomicInteger
            implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        volatile boolean cancelled;

        CompletableFuture<Boolean> completable;

        T result;

        boolean once;

        boolean hasValue;

        LastEnumerator(AsyncEnumerator<T> source) {
            this.source = source;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (once) {
                result = null;
                return FALSE;
            }
            once = true;
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            nextSource();
            return cf;
        }

        void nextSource() {
            if (getAndIncrement() == 0) {
                do {
                    source.moveNext().whenComplete(this);
                } while (decrementAndGet() != 0);
            }
        }

        @Override
        public T current() {
            return result;
        }

        @Override
        public void cancel() {
            cancelled = true;
            source.cancel();
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                completable.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                if (!hasValue) {
                    hasValue = true;
                }
                result = source.current();
                nextSource();
            } else {
                completable.complete(hasValue);
            }
        }
    }
}

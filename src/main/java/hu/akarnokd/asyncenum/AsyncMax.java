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

import java.util.Comparator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

final class AsyncMax<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Comparator<? super T> comparator;

    AsyncMax(AsyncEnumerable<T> source, Comparator<? super T> comparator) {
        this.source = source;
        this.comparator = comparator;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new SumLongEnumerator<>(source.enumerator(), comparator);
    }

    static final class SumLongEnumerator<T> extends AtomicInteger
    implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Comparator<? super T> comparator;

        boolean hasValue;
        T max;

        T result;
        boolean done;

        CompletableFuture<Boolean> completable;

        volatile boolean cancelled;

        SumLongEnumerator(AsyncEnumerator<T> source, Comparator<? super T> comparator) {
            this.source = source;
            this.comparator = comparator;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (done) {
                result = null;
                return FALSE;
            }
            completable = new CompletableFuture<>();
            collectSource();
            return completable;
        }

        @Override
        public T current() {
            return result;
        }

        void collectSource() {
            if (getAndIncrement() == 0) {
                do {
                    if (cancelled) {
                        return;
                    }
                    source.moveNext().whenComplete(this);
                } while (decrementAndGet() != 0);
            }
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                done = true;
                completable.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                if (hasValue) {
                    T curr = source.current();
                    if (comparator.compare(max, curr) <= 0) {
                        max = curr;
                    }
                } else {
                    hasValue = true;
                    max = source.current();
                }
                collectSource();
            } else {
                done = true;
                if (hasValue) {
                    result = max;
                    completable.complete(true);
                } else {
                    completable.complete(false);
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            source.cancel();
        }
    }
}

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
import java.util.function.*;

final class AsyncReduce<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final BiFunction<T, T, T> reducer;

    AsyncReduce(AsyncEnumerable<T> source, BiFunction<T, T, T> reducer) {
        this.source = source;
        this.reducer = reducer;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new ReduceEnumerator<>(source.enumerator(), reducer);
    }

    static final class ReduceEnumerator<T>
            extends AtomicInteger
            implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final BiFunction<T, T, T> reducer;

        boolean once;

        boolean moveNextOnce;

        T accumulator;

        T result;

        CompletableFuture<Boolean> completable;

        volatile boolean cancelled;

        ReduceEnumerator(AsyncEnumerator<T> source, BiFunction<T, T, T> reducer) {
            this.source = source;
            this.reducer = reducer;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (moveNextOnce) {
                result = null;
                return FALSE;
            }
            moveNextOnce = true;
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            nextSource();
            return cf;
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

        void nextSource() {
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
                accumulator = null;
                completable.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                if (once) {
                    accumulator = reducer.apply(accumulator, source.current());
                } else {
                    once = true;
                    accumulator = source.current();
                }
                nextSource();
            } else {
                result = accumulator;
                accumulator = null;
                completable.complete(once);
            }
        }
    }
}

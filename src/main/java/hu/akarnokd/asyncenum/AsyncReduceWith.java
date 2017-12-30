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

final class AsyncReduceWith<T, R> implements AsyncEnumerable<R> {

    final AsyncEnumerable<T> source;

    final Supplier<R> initialSupplier;

    final BiFunction<R, T, R> reducer;

    AsyncReduceWith(AsyncEnumerable<T> source, Supplier<R> initialSupplier, BiFunction<R, T, R> reducer) {
        this.source = source;
        this.initialSupplier = initialSupplier;
        this.reducer = reducer;
    }

    @Override
    public AsyncEnumerator<R> enumerator() {
        return new ReduceWithEnumerator<>(source.enumerator(), reducer, initialSupplier.get());
    }

    static final class ReduceWithEnumerator<T, R>
            extends AtomicInteger
            implements AsyncEnumerator<R>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final BiFunction<R, T, R> reducer;

        R accumulator;

        R result;

        boolean once;

        CompletableFuture<Boolean> completable;

        volatile boolean cancelled;

        ReduceWithEnumerator(AsyncEnumerator<T> source, BiFunction<R, T, R> reducer, R accumulator) {
            this.source = source;
            this.reducer = reducer;
            this.accumulator = accumulator;
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
                    if (cancelled) {
                        return;
                    }
                    source.moveNext().whenComplete(this);
                } while (decrementAndGet() != 0);
            }
        }

        @Override
        public R current() {
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
                accumulator = null;
                completable.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                accumulator = reducer.apply(accumulator, source.current());
                nextSource();
            } else {
                result = accumulator;
                accumulator = null;
                completable.complete(true);
            }
        }
    }
}

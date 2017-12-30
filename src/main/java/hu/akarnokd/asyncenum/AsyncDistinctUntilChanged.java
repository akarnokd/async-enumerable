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

final class AsyncDistinctUntilChanged<T, K> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Function<? super T, ? extends K> keySelector;

    final BiPredicate<? super K, ? super K> comparer;

    AsyncDistinctUntilChanged(AsyncEnumerable<T> source, Function<? super T, ? extends K> keySelector, BiPredicate<? super K, ? super K> comparer) {
        this.source = source;
        this.keySelector = keySelector;
        this.comparer = comparer;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new DistinctUntilChangedEnumerator<>(source.enumerator(), keySelector, comparer);
    }

    static final class DistinctUntilChangedEnumerator<T, K>
            extends AtomicInteger
            implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Function<? super T, ? extends K> keySelector;

        final BiPredicate<? super K, ? super K> comparer;

        CompletableFuture<Boolean> completable;

        volatile boolean cancelled;

        T result;

        boolean once;
        K currentKey;

        DistinctUntilChangedEnumerator(AsyncEnumerator<T> source, Function<? super T, ? extends K> keySelector, BiPredicate<? super K, ? super K> comparer) {
            this.source = source;
            this.keySelector = keySelector;
            this.comparer = comparer;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            result = null;
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
                currentKey = null;
                completable.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                T v = source.current();
                if (once) {
                    K prevKey = currentKey;
                    K nextKey = keySelector.apply(v);

                    if (comparer.test(prevKey, nextKey)) {
                        currentKey = nextKey;
                        nextSource();
                    } else {
                        currentKey = nextKey;
                        result = v;
                        completable.complete(true);
                    }
                } else {
                    once = true;
                    currentKey = keySelector.apply(v);
                    result = v;
                    completable.complete(true);
                }
            } else {
                currentKey = null;
                completable.complete(false);
            }
        }
    }
}

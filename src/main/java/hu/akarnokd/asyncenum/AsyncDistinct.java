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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

final class AsyncDistinct<T, K, C extends Collection<? super K>> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Function<? super T, ? extends K> keySelector;

    final Supplier<C> setSupplier;

    AsyncDistinct(AsyncEnumerable<T> source, Function<? super T, ? extends K> keySelector, Supplier<C> setSupplier) {
        this.source = source;
        this.keySelector = keySelector;
        this.setSupplier = setSupplier;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new DistinctEnumerator<>(source.enumerator(), keySelector, setSupplier.get());
    }

    static final class DistinctEnumerator<T, K>
            extends AtomicInteger
            implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Function<? super T, ? extends K> keySelector;

        final Collection<? super K> set;

        CompletableFuture<Boolean> completable;

        T result;

        volatile boolean cancelled;

        DistinctEnumerator(AsyncEnumerator<T> source, Function<? super T, ? extends K> keySelector, Collection<? super K> set) {
            this.source = source;
            this.keySelector = keySelector;
            this.set = set;
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
                set.clear();
                completable.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                T v = source.current();
                if (set.add(keySelector.apply(v))) {
                    result = v;
                    completable.complete(true);
                } else {
                    nextSource();
                }
            } else {
                set.clear();
                completable.complete(false);
            }

        }
    }
}

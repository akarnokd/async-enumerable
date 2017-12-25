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

final class AsyncFilter<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Predicate<? super T> predicate;

    AsyncFilter(AsyncEnumerable<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new FilterEnumerator<>(source.enumerator(), predicate);
    }

    static final class FilterEnumerator<T> extends AtomicInteger implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Predicate<? super T> predicate;

        CompletableFuture<Boolean> current;

        T currentItem;

        FilterEnumerator(AsyncEnumerator<T> source, Predicate<? super T> predicate) {
            this.source = source;
            this.predicate = predicate;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            current = new CompletableFuture<>();
            moveNextSource();
            return current;
        }

        @Override
        public T current() {
            return currentItem;
        }

        void moveNextSource() {
            if (getAndIncrement() == 0) {
                do {
                    source.moveNext().whenComplete(this);
                } while (decrementAndGet() != 0);
            }
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                currentItem = null;
                current.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                T v = source.current();
                if (predicate.test(v)) {
                    currentItem = v;
                    current.complete(true);
                } else {
                    currentItem = null;
                    moveNextSource();
                }
            } else {
                current.complete(false);
            }
        }
    }
}

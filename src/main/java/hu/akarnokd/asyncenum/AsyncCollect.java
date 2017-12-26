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

final class AsyncCollect<T, C> implements AsyncEnumerable<C> {

    final AsyncEnumerable<T> source;

    final Supplier<C> supplier;

    final BiConsumer<C, T> collector;

    AsyncCollect(AsyncEnumerable<T> source, Supplier<C> supplier, BiConsumer<C, T> collector) {
        this.source = source;
        this.supplier = supplier;
        this.collector = collector;
    }

    @Override
    public AsyncEnumerator<C> enumerator() {
        return new CollectEnumerator<>(source.enumerator(), collector, supplier.get());
    }

    static final class CollectEnumerator<T, C> extends AtomicInteger
    implements AsyncEnumerator<C>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final BiConsumer<C, T> collector;

        C collection;

        C result;

        CompletableFuture<Boolean> cf;

        CollectEnumerator(AsyncEnumerator<T> source, BiConsumer<C, T> collector, C collection) {
            this.source = source;
            this.collector = collector;
            this.collection = collection;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (collection == null) {
                result = null;
                return FALSE;
            }
            cf = new CompletableFuture<>();
            collectSource();
            return cf;
        }

        @Override
        public C current() {
            return result;
        }

        void collectSource() {
            if (getAndIncrement() == 0) {
                do {
                    source.moveNext().whenComplete(this);
                } while (decrementAndGet() != 0);
            }
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                collection = null;
                cf.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                collector.accept(collection, source.current());
                collectSource();
            } else {
                result = collection;
                collection = null;
                cf.complete(true);
            }
        }

        @Override
        public void cancel() {
            source.cancel();
        }
    }
}

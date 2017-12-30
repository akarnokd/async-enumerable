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
import java.util.stream.Collector;

final class AsyncCollectWith<T, A, R> implements AsyncEnumerable<R> {

    final AsyncEnumerable<T> source;

    final Collector<T, A, R> collector;

    AsyncCollectWith(AsyncEnumerable<T> source, Collector<T, A, R> collector) {
        this.source = source;
        this.collector = collector;
    }

    @Override
    public AsyncEnumerator<R> enumerator() {
        return new CollectWithEnumerator<>(source.enumerator(),
                    collector.supplier().get(),
                    collector.accumulator(),
                    collector.finisher()
                );
    }

    static final class CollectWithEnumerator<T, A, R> extends AtomicInteger
    implements AsyncEnumerator<R>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final BiConsumer<A, T> accumulator;

        final Function<A, R> finisher;

        A collection;

        R result;

        CompletableFuture<Boolean> cf;

        CollectWithEnumerator(AsyncEnumerator<T> source, A collection, BiConsumer<A, T> accumulator, Function<A, R> finisher) {
            this.source = source;
            this.collection = collection;
            this.accumulator = accumulator;
            this.finisher = finisher;
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
        public R current() {
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
                accumulator.accept(collection, source.current());
                collectSource();
            } else {
                result = finisher.apply(collection);
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

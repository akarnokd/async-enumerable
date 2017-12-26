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

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

final class AsyncZipArray<T, R> implements AsyncEnumerable<R> {

    final AsyncEnumerable<? extends T>[] sources;

    final Function<? super Object[], ? extends R> zipper;

    AsyncZipArray(AsyncEnumerable<? extends T>[] sources, Function<? super Object[], ? extends R> zipper) {
        this.sources = sources;
        this.zipper = zipper;
    }

    @Override
    public AsyncEnumerator<R> enumerator() {
        return new ZipArrayEnumerator<>(sources, zipper);
    }

    static final class ZipArrayEnumerator<T, R> extends AtomicInteger implements AsyncEnumerator<R> {

        final AsyncEnumerator<? extends T>[] sources;

        final Function<? super Object[], ? extends R> zipper;

        final Object[] results;

        final ZipInnerConsumer[] consumers;

        CompletableFuture<Boolean> completable;

        R result;

        @SuppressWarnings("unchecked")
        ZipArrayEnumerator(AsyncEnumerable<? extends T>[] sources, Function<? super Object[], ? extends R> zipper) {
            int n = sources.length;
            this.sources = new AsyncEnumerator[n];
            this.zipper = zipper;
            this.results = new Object[n];
            this.consumers = new ZipInnerConsumer[n];
            for (int i = 0; i < n; i++) {
                this.sources[i] = sources[i].enumerator();
                consumers[i] = new ZipInnerConsumer(i, this);
            }
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            result = null;
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            int n = results.length;
            set(n);
            for (int i = 0; i < n; i++) {
                sources[i].moveNext().whenComplete(consumers[i]);
            }
            return cf;
        }

        @Override
        public R current() {
            return result;
        }

        void acceptInner(int index, Boolean hasValue, Throwable throwable) {
            CompletableFuture<Boolean> cf = completable;

            if (throwable != null) {
                completable = null;
                for (int i = 0; i < sources.length; i++) {
                    if (i != index) {
                        sources[i].cancel();
                    }
                }
                cf.completeExceptionally(throwable);
                return;
            }

            if (hasValue) {
                results[index] = sources[index].current();
                if (decrementAndGet() == 0) {
                    result = zipper.apply(results.clone());
                    Arrays.fill(results, null);
                    cf.complete(true);
                }
            } else {
                result = null;
                for (int i = 0; i < sources.length; i++) {
                    if (i != index) {
                        sources[i].cancel();
                    }
                }
                cf.complete(false);
            }
        }

        @Override
        public void cancel() {
            for (AsyncEnumerator<? extends T> source : sources) {
                source.cancel();
            }
        }

        static final class ZipInnerConsumer implements BiConsumer<Boolean, Throwable> {

            final int index;

            final ZipArrayEnumerator<?, ?> parent;

            ZipInnerConsumer(int index, ZipArrayEnumerator<?, ?> parent) {
                this.index = index;
                this.parent = parent;
            }

            @Override
            public void accept(Boolean aBoolean, Throwable throwable) {
                parent.acceptInner(index, aBoolean, throwable);
            }
        }
    }
}

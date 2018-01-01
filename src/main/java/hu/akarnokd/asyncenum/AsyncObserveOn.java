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

final class AsyncObserveOn<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Executor executor;

    AsyncObserveOn(AsyncEnumerable<T> source, Executor executor) {
        this.source = source;
        this.executor = executor;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new ObserveOnEnumerator<>(source.enumerator(), executor);
    }

    static final class ObserveOnEnumerator<T> implements AsyncEnumerator<T> {

        final AsyncEnumerator<T> source;

        final Executor executor;

        ObserveOnEnumerator(AsyncEnumerator<T> source, Executor executor) {
            this.source = source;
            this.executor = executor;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            AsyncCompletableFuture<Boolean> cf = new AsyncCompletableFuture<>();
            source.moveNext().whenCompleteAsync(cf, executor);
            return cf;
        }

        @Override
        public T current() {
            return source.current();
        }

        @Override
        public void cancel() {
            source.cancel();
        }
    }

}

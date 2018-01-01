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

final class AsyncSubscribeOn<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Executor executor;

    AsyncSubscribeOn(AsyncEnumerable<T> source, Executor executor) {
        this.source = source;
        this.executor = executor;
    }


    @Override
    public AsyncEnumerator<T> enumerator() {
        SubscribeOnEnumerator<T> en = new SubscribeOnEnumerator<>(source);
        executor.execute(en);
        return en;
    }

    static final class SubscribeOnEnumerator<T> implements AsyncEnumerator<T>, Runnable {

        final CompletableFuture<AsyncEnumerator<T>> source;

        final AsyncEnumerable<T> upstream;

        SubscribeOnEnumerator(AsyncEnumerable<T> upstream) {
            this.upstream = upstream;
            this.source = new CompletableFuture<>();
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            AsyncEnumerator<T> en = source.getNow(null);
            if (en != null) {
                return en.moveNext();
            }
            return source.thenCompose(AsyncEnumerator::moveNext);
        }

        @Override
        public T current() {
            AsyncEnumerator<T> en = source.getNow(null);
            return en != null ? en.current() : null;
        }

        @Override
        public void run() {
            AsyncEnumerator<T> en = upstream.enumerator();
            if (!source.complete(en)) {
                en.cancel();
            }
        }

        @Override
        public void cancel() {
            if (!source.completeExceptionally(new CancellationException())) {
                AsyncEnumerator<T> en = source.getNow(null);
                if (en != null) {
                    en.cancel();
                }
            }
        }
    }
}

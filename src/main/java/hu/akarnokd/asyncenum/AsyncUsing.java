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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

final class AsyncUsing<T, U> implements AsyncEnumerable<T> {

    final Supplier<U> resource;

    final Function<? super U, ? extends AsyncEnumerable<T>> handler;

    final Consumer<? super U> releaseResource;

    AsyncUsing(Supplier<U> resource, Function<? super U, ? extends AsyncEnumerable<T>> handler, Consumer<? super U> releaseResource) {
        this.resource = resource;
        this.handler = handler;
        this.releaseResource = releaseResource;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        U res = resource.get();
        return new UsingEnumerator<>(res, handler.apply(res).enumerator(), releaseResource);
    }

    static final class UsingEnumerator<T, U>
            extends AtomicBoolean
            implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final U resource;

        final AsyncEnumerator<T> source;

        final Consumer<? super U> release;

        T result;

        CompletableFuture<Boolean> completable;

        UsingEnumerator(U resource, AsyncEnumerator<T> source, Consumer<? super U> release) {
            this.resource = resource;
            this.source = source;
            this.release = release;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            result = null;
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            source.moveNext().whenComplete(this);
            return cf;
        }

        @Override
        public T current() {
            return result;
        }

        @Override
        public void cancel() {
            source.cancel();
            cleanup();
        }

        void cleanup() {
            if (compareAndSet(false, true)) {
                release.accept(resource);
            }
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                completable.completeExceptionally(throwable);
                cleanup();
                return;
            }

            if (aBoolean) {
                result = source.current();
                completable.complete(true);
            } else {
                completable.complete(false);
                cleanup();
            }
        }
    }
}

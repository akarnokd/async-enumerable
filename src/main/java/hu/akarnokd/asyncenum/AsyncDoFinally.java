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
import java.util.function.BiConsumer;

final class AsyncDoFinally<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Runnable onFinally;

    AsyncDoFinally(AsyncEnumerable<T> source, Runnable onFinally) {
        this.source = source;
        this.onFinally = onFinally;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new DoFinallyEnumerator<>(source.enumerator(), onFinally);
    }

    static final class DoFinallyEnumerator<T>
            extends AtomicBoolean
            implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Runnable onFinally;

        CompletableFuture<Boolean> completable;

        T result;

        DoFinallyEnumerator(AsyncEnumerator<T> source, Runnable onFinally) {
            this.source = source;
            this.onFinally = onFinally;
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
            runFinally();
        }

        void runFinally() {
            if (compareAndSet(false, true)) {
                onFinally.run();
            }
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                completable.completeExceptionally(throwable);
                runFinally();
                return;
            }

            if (aBoolean) {
                result = source.current();
                completable.complete(true);
            } else {
                completable.complete(false);
                runFinally();
            }
        }
    }
}

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
import java.util.function.*;

final class AsyncDoOn<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Consumer<? super T> onNext;

    final Consumer<? super Throwable> onError;

    final Runnable onComplete;

    AsyncDoOn(AsyncEnumerable<T> source, Consumer<? super T> onNext, Consumer<? super Throwable> onError, Runnable onComplete) {
        this.source = source;
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new DoOnEnumerator<>(source.enumerator(), onNext, onError, onComplete);
    }

    static final class DoOnEnumerator<T> implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Consumer<? super T> onNext;

        final Consumer<? super Throwable> onError;

        final Runnable onComplete;

        CompletableFuture<Boolean> completable;

        T result;

        DoOnEnumerator(AsyncEnumerator<T> source, Consumer<? super T> onNext,
                       Consumer<? super Throwable> onError, Runnable onComplete) {
            this.source = source;
            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            result = null;
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            // Note that returning this directly results in CompletionException(Throwable)
            // instead of the original failure for some reason.
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
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                onError.accept(throwable);
                completable.completeExceptionally(throwable);
                return;
            }
            if (aBoolean) {
                T r = source.current();
                result = r;
                onNext.accept(r);
                completable.complete(true);
            } else {
                onComplete.run();
                completable.complete(false);
            }
        }
    }
}

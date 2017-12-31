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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

final class AsyncSwitchIfEmpty<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final AsyncEnumerable<T> fallback;

    AsyncSwitchIfEmpty(AsyncEnumerable<T> source, AsyncEnumerable<T> fallback) {
        this.source = source;
        this.fallback = fallback;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new SwitchIfEmptyEnumerator<>(source.enumerator(), fallback);
    }

    static final class SwitchIfEmptyEnumerator<T>
            implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AtomicReference<AsyncEnumerator<T>> source;

        AsyncEnumerable<T> fallback;

        CompletableFuture<Boolean> completable;

        T result;

        boolean hasValue;

        SwitchIfEmptyEnumerator(AsyncEnumerator<T> source, AsyncEnumerable<T> fallback) {
            this.source = new AtomicReference<>(source);
            this.fallback = fallback;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            result = null;
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            source.getPlain().moveNext().whenComplete(this);
            return cf;
        }

        @Override
        public T current() {
            return result;
        }

        @Override
        public void cancel() {
            AsyncEnumeratorHelper.cancel(source);
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                completable.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                if (!hasValue) {
                    hasValue = true;
                }
                result = source.getPlain().current();
                completable.complete(true);
            } else {
                if (hasValue) {
                    completable.complete(false);
                } else {
                    hasValue = true;
                    AsyncEnumerator<T> fb = fallback.enumerator();
                    fallback = null;
                    if (AsyncEnumeratorHelper.replace(source, fb)) {
                        fb.moveNext().whenComplete(this);
                    }
                }
            }
        }
    }
}


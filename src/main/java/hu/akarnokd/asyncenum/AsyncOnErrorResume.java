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
import java.util.function.*;

final class AsyncOnErrorResume<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Function<? super Throwable, ? extends AsyncEnumerable<? extends T>> resumeMapper;

    AsyncOnErrorResume(AsyncEnumerable<T> source, Function<? super Throwable, ? extends AsyncEnumerable<? extends T>> resumeMapper) {
        this.source = source;
        this.resumeMapper = resumeMapper;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new OnErrorResumeEnumerator<>(source.enumerator(), resumeMapper);
    }

    static final class OnErrorResumeEnumerator<T> implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final Function<? super Throwable, ? extends AsyncEnumerable<? extends T>> resumeMapper;

        final AtomicReference<AsyncEnumerator<T>> source;

        T result;

        CompletableFuture<Boolean> completable;

        boolean inFallback;

        OnErrorResumeEnumerator(AsyncEnumerator<T> source, Function<? super Throwable, ? extends AsyncEnumerable<? extends T>> resumeMapper) {
            this.source = new AtomicReference<>(source);
            this.resumeMapper = resumeMapper;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
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
        @SuppressWarnings("unchecked")
        public void accept(Boolean aBoolean, Throwable throwable) {
            CompletableFuture<Boolean> cf = completable;
            if (inFallback) {
                if (throwable != null) {
                    result = null;
                    cf.completeExceptionally(throwable);
                    return;
                }
            } else {
                if (throwable != null) {
                    inFallback = true;
                    result = null;
                    if (AsyncEnumeratorHelper.replace(source, (AsyncEnumerator<T>)resumeMapper.apply(throwable).enumerator())) {
                        source.getPlain().moveNext().whenComplete(this);
                    }
                    return;
                }
            }
            if (aBoolean) {
                result = source.getPlain().current();
                cf.complete(true);
            } else {
                result = null;
                cf.complete(false);
            }
        }

        @Override
        public void cancel() {
            AsyncEnumeratorHelper.cancel(source);
        }
    }
}

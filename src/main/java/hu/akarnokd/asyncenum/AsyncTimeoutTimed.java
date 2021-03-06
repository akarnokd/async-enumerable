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
import java.util.concurrent.atomic.*;
import java.util.function.BiConsumer;

final class AsyncTimeoutTimed<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final long timeout;

    final TimeUnit unit;

    final ScheduledExecutorService executor;

    final AsyncEnumerable<T> fallback;

    AsyncTimeoutTimed(AsyncEnumerable<T> source, long timeout, TimeUnit unit, ScheduledExecutorService executor, AsyncEnumerable<T> fallback) {
        this.source = source;
        this.timeout = timeout;
        this.unit = unit;
        this.executor = executor;
        this.fallback = fallback;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new TimeoutTimedEnumerator<>(source.enumerator(), timeout, unit, executor, fallback);
    }

    static final class TimeoutTimedEnumerator<T>
            implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final long timeout;

        final TimeUnit unit;

        final ScheduledExecutorService executor;

        final AsyncEnumerable<T> fallback;

        final AtomicLong index;

        final AtomicReference<AsyncEnumerator<T>> source;

        volatile CompletableFuture<Boolean> completable;

        Future<?> future;

        T result;

        TimeoutTimedEnumerator(AsyncEnumerator<T> source, long timeout, TimeUnit unit, ScheduledExecutorService executor, AsyncEnumerable<T> fallback) {
            this.source = new AtomicReference<>(source);
            this.timeout = timeout;
            this.unit = unit;
            this.executor = executor;
            this.fallback = fallback;
            this.index = new AtomicLong();
        }


        @Override
        public CompletionStage<Boolean> moveNext() {
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            AsyncEnumerator<T> en = source.getPlain();
            long idx = index.get();
            if (idx != Long.MAX_VALUE) {
                future = executor.schedule(() -> timeout(idx), timeout, unit);
                en.moveNext().whenComplete(this);
            } else {
                en.moveNext().whenComplete(this::acceptFallback);
            }
            return cf;
        }

        @Override
        public T current() {
            return result;
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            future.cancel(false);
            long idx = index.get();
            if (idx != Long.MAX_VALUE && index.compareAndSet(idx, idx + 1)) {
                acceptFallback(aBoolean, throwable);
            }
        }

        public void acceptFallback(Boolean aBoolean, Throwable throwable) {
            CompletableFuture<Boolean> cf = completable;
            if (throwable != null) {
                cf.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                result = source.getPlain().current();
                cf.complete(true);
            } else {
                cf.complete(false);
            }
        }

        void timeout(long index) {
            if (this.index.compareAndSet(index, Long.MAX_VALUE)) {
                source.getPlain().cancel();
                if (fallback != null) {
                    if (AsyncEnumeratorHelper.replace(source, fallback.enumerator())) {
                        source.getPlain().moveNext().whenComplete(this::acceptFallback);
                    }
                } else {
                    completable.completeExceptionally(new TimeoutException());
                }
            }
        }

        @Override
        public void cancel() {
            AsyncEnumeratorHelper.cancel(source);
        }
    }
}

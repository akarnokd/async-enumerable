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
import java.util.function.*;

final class AsyncPublish<T, R> implements AsyncEnumerable<R> {

    final AsyncEnumerable<T> source;

    final Function<? super AsyncEnumerable<T>, ? extends AsyncEnumerable<R>> handler;

    AsyncPublish(AsyncEnumerable<T> source, Function<? super AsyncEnumerable<T>, ? extends AsyncEnumerable<R>> handler) {
        this.source = source;
        this.handler = handler;
    }

    @Override
    public AsyncEnumerator<R> enumerator() {
        PublishCoordinator<T, R> coordinator = new PublishCoordinator<>();
        coordinator.output = handler.apply(coordinator).enumerator();
        coordinator.source = source.enumerator();
        coordinator.enumeratorReady();
        return coordinator;
    }

    static final class PublishCoordinator<T, R> implements BiConsumer<Boolean, Throwable>, AsyncEnumerable<T>, AsyncEnumerator<R> {

        volatile AsyncEnumerator<T> source;

        AsyncEnumerator<R> output;

        final AtomicReference<PublishEnumerator<T, R>[]> enumerators;

        static final PublishEnumerator[] EMPTY = new PublishEnumerator[0];

        static final PublishEnumerator[] TERMINATED = new PublishEnumerator[0];

        final AtomicInteger enumeratorWip;

        final AtomicInteger sourceWip;

        final AtomicInteger outputWip;

        volatile boolean cancelled;

        volatile CompletableFuture<Boolean> outputCompletable;

        R outputResult;

        volatile boolean sourceDone;
        volatile Throwable sourceError;

        @SuppressWarnings("unchecked")
        PublishCoordinator() {
            enumerators = new AtomicReference<>(EMPTY);
            enumeratorWip = new AtomicInteger();
            sourceWip = new AtomicInteger();
            outputWip = new AtomicInteger();
        }

        boolean add(PublishEnumerator<T, R> en) {
            for (;;) {
                PublishEnumerator<T, R>[] a = enumerators.getAcquire();
                if (a == TERMINATED) {
                    return false;
                }
                int n = a.length;
                @SuppressWarnings("unchecked")
                PublishEnumerator<T, R>[] b = new PublishEnumerator[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = en;
                if (enumerators.compareAndSet(a, b)) {
                    return true;
                }
            }
        }

        @SuppressWarnings("unchecked")
        void remove(PublishEnumerator<T, R> en) {
            for (;;) {
                PublishEnumerator<T, R>[] a = enumerators.getAcquire();
                int n = a.length;
                if (n == 0) {
                    return;
                }

                int j = -1;
                for (int i = 0; i < n; i++) {
                    if (a[i] == en) {
                        j = i;
                        break;
                    }
                }

                if (j < 0) {
                    break;
                }
                PublishEnumerator<T, R>[] b;
                if (n == 1) {
                    b = EMPTY;
                } else {
                    b = new PublishEnumerator[n - 1];
                    System.arraycopy(a, 0, b, 0, j);
                    System.arraycopy(a, j + 1, b, j, n - j - 1);
                }
                if (enumerators.compareAndSet(a, b)) {
                    break;
                }
            }
        }

        void enumeratorReady() {
            if (enumeratorWip.getAndIncrement() == 0) {
                do {
                    AsyncEnumerator<T> en = source;
                    if (en != null) {
                        PublishEnumerator<T, R>[] ens = enumerators.getAcquire();
                        boolean canRequest = ens.length != 0;
                        for (PublishEnumerator<T, R> pe : ens) {
                            if (!pe.cancelled && pe.requested == pe.emitted) {
                                canRequest = false;
                                break;
                            }
                        }

                        if (canRequest) {
                            for (PublishEnumerator<T, R> pe : ens) {
                                pe.emitted++;
                            }
                            nextSource(en);
                        }
                    }
                } while (enumeratorWip.decrementAndGet() != 0);
            }
        }

        void nextSource(AsyncEnumerator<T> en) {
            if (sourceWip.getAndIncrement() == 0) {
                do {
                    if (cancelled) {
                        return;
                    }
                    en.moveNext().whenComplete(this);
                } while (sourceWip.decrementAndGet() != 0);
            }
        }

        @Override
        public AsyncEnumerator<T> enumerator() {
            PublishEnumerator<T, R> pe = new PublishEnumerator<>(this);
            if (!add(pe)) {
                pe.error = sourceError;
                pe.done = sourceDone;
            }
            pe.drain();
            return pe;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            outputCompletable = cf;
            nextOutput();
            return cf;
        }

        @Override
        public R current() {
            return outputResult;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                sourceError = throwable;
                for (PublishEnumerator<T, R> en : enumerators.getAndSet(TERMINATED)) {
                    en.error = throwable;
                    en.drain();
                }
                return;
            }

            if (aBoolean) {
                T v = source.current();
                for (PublishEnumerator<T, R> en : enumerators.getAcquire()) {
                    en.result = v;
                    en.hasResult = true;
                    en.drain();
                }
            } else {
                sourceDone = true;
                for (PublishEnumerator<T, R> en : enumerators.getAndSet(TERMINATED)) {
                    en.done = true;
                    en.drain();
                }
            }
        }

        public void acceptOutput(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                source.cancel();
                outputCompletable.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                outputResult = output.current();
                outputCompletable.complete(true);
            } else {
                source.cancel();
                outputCompletable.complete(false);
            }
        }

        void nextOutput() {
            if (outputWip.getAndIncrement() == 0) {
                do {
                    if (cancelled) {
                        return;
                    }
                    output.moveNext().whenComplete(this::acceptOutput);
                } while (outputWip.decrementAndGet() != 0);
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            source.cancel();
            output.cancel();
        }

        static final class PublishEnumerator<T, R> extends AtomicInteger implements AsyncEnumerator<T> {

            final PublishCoordinator<T, R> parent;

            volatile long requested;
            long emitted;

            volatile CompletableFuture<Boolean> completable;

            T result;
            volatile boolean hasResult;
            volatile boolean done;
            volatile Throwable error;

            volatile boolean cancelled;

            boolean once;

            PublishEnumerator(PublishCoordinator<T, R> parent) {
                this.parent = parent;
            }

            @Override
            public CompletionStage<Boolean> moveNext() {
                if (once) {
                    result = null;
                    hasResult = false;
                } else {
                    once = true;
                }
                CompletableFuture<Boolean> cf = new CompletableFuture<>();
                completable = cf;
                requested = requested + 1;
                parent.enumeratorReady();
                drain();
                return cf;
            }

            @Override
            public T current() {
                return result;
            }

            @Override
            public void cancel() {
                cancelled = true;
                parent.remove(this);
                parent.enumeratorReady();
            }

            void drain() {
                if (getAndIncrement() == 0) {
                    do {
                        CompletableFuture<Boolean> cf = completable;
                        if (cf != null) {
                            Throwable ex = error;
                            if (ex != null) {
                                completable = null;
                                cf.completeExceptionally(ex);
                                return;
                            }
                            if (done) {
                                completable = null;
                                cf.complete(false);
                                return;
                            }
                            if (hasResult) {
                                completable = null;
                                cf.complete(true);
                            }
                        }
                    } while (decrementAndGet() != 0);
                }
            }
        }
    }
}

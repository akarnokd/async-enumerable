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
import java.util.function.Consumer;

final class AsyncCreate<T> implements AsyncEnumerable<T> {

    final Consumer<AsyncEmitter<T>> emitter;

    AsyncCreate(Consumer<AsyncEmitter<T>> emitter) {
        this.emitter = emitter;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        CreateEnumerator<T> en = new CreateEnumerator<>();
        emitter.accept(en);
        return en;
    }

    enum Closed implements AutoCloseable {
        INSTANCE;

        @Override
        public void close() {
            // deliberately no-op
        }
    }

    static final class CreateEnumerator<T>
            extends AtomicInteger
            implements AsyncEmitter<T>, AsyncEnumerator<T> {

        final AtomicReference<AutoCloseable> res;

        final ConcurrentLinkedQueue<T> queue;

        T result;
        volatile boolean done;
        Throwable error;

        volatile CompletableFuture<Boolean> completable;

        AutoCloseable toRelease;

        CreateEnumerator() {
            queue = new ConcurrentLinkedQueue<>();
            res = new AtomicReference<>();
        }

        @Override
        public void cancel() {
            AutoCloseable c = res.getAndSet(Closed.INSTANCE);
            if (c != Closed.INSTANCE) {
                closeSilently(c);
            }
        }

        final void closeSilently(AutoCloseable c) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ex) {
                    Thread t = Thread.currentThread();
                    t.getUncaughtExceptionHandler().uncaughtException(t, ex);
                }
            }
        }

        @Override
        public final void setResource(AutoCloseable resource) {
            for (;;) {
                AutoCloseable c = res.getAcquire();
                if (c == Closed.INSTANCE) {
                    closeSilently(resource);
                    break;
                }
                if (res.compareAndSet(c, resource)) {
                    closeSilently(c);
                    break;
                }
            }
        }

        public final boolean isCancelled() {
            return res.getAcquire() == Closed.INSTANCE;
        }

        @Override
        public int emissionPending() {
            return queue.size();
        }

        @Override
        public void next(T item) {
            if (!isCancelled()) {
                queue.offer(item);
                drain();
            }
        }

        @Override
        public void error(Throwable error) {
            if (!isCancelled()) {
                toRelease = res.getAndSet(Closed.INSTANCE);
                this.error = error;
                this.done = true;
                drain();
            }
        }

        @Override
        public void stop() {
            if (!isCancelled()) {
                toRelease = res.getAndSet(Closed.INSTANCE);
                this.done = true;
                drain();
            }
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            result = null;
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            drain();
            return cf;
        }

        @Override
        public T current() {
            return result;
        }


        void drain() {
            if (getAndIncrement() == 0) {
                do {
                    CompletableFuture<Boolean> cf = completable;
                    if (cf != null) {
                        boolean d = done;
                        boolean empty = queue.isEmpty();

                        if (d && empty) {
                            completable = null;
                            Throwable ex = error;
                            if (ex == null) {
                                cf.complete(false);
                            } else {
                                cf.completeExceptionally(ex);
                            }
                            AutoCloseable c = toRelease;
                            toRelease = null;
                            if (c != null) {
                                closeSilently(c);
                            }
                            return;
                        }

                        if (!empty) {
                            completable = null;
                            result = queue.poll();
                            cf.complete(true);
                        }
                    }
                } while (decrementAndGet() != 0);
            }
        }
    }
}

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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BiConsumer;

final class AsyncCache<T> extends AtomicInteger
        implements AsyncEnumerable<T>, BiConsumer<Boolean, Throwable> {

    final AsyncEnumerable<T> source;

    final AtomicBoolean once;

    final AtomicReference<CacheEnumerator<T>[]> enumerators;

    @SuppressWarnings("unchecked")
    static final CacheEnumerator[] EMPTY = new CacheEnumerator[0];

    @SuppressWarnings("unchecked")
    static final CacheEnumerator[] TERMINATED = new CacheEnumerator[0];

    final List<T> list;

    AsyncEnumerator<T> sourceEnumerator;

    volatile int size;

    volatile boolean done;
    Throwable error;

    @SuppressWarnings("unchecked")
    AsyncCache(AsyncEnumerable<T> source) {
        this.source = source;
        this.once = new AtomicBoolean();
        this.enumerators = new AtomicReference<>(EMPTY);
        this.list = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void accept(Boolean aBoolean, Throwable throwable) {
        if (throwable != null) {
            error = throwable;
            done = true;
            for (CacheEnumerator<T> en : enumerators.getAndSet(TERMINATED)) {
                signal(en);
            }
            return;
        }

        if (aBoolean) {
            list.add(sourceEnumerator.current());
            size = size + 1;
            for (CacheEnumerator<T> en : enumerators.getAcquire()) {
                signal(en);
            }
            nextSource();
        } else {
            done = true;
            for (CacheEnumerator<T> en : enumerators.getAndSet(TERMINATED)) {
                signal(en);
            }
        }
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        CacheEnumerator<T> en = new CacheEnumerator<>(this);
        if (add(en)) {
            if (!once.get() && once.compareAndSet(false, true)) {
                sourceEnumerator = source.enumerator();
                nextSource();
            }
        }
        signal(en);
        return en;
    }

    void nextSource() {
        if (getAndIncrement() == 0) {
            do {
                sourceEnumerator.moveNext().whenComplete(this);
            } while (decrementAndGet() != 0);
        }
    }

    boolean add(CacheEnumerator<T> inner) {
        for (;;) {
            CacheEnumerator<T>[] a = enumerators.get();
            if (a == TERMINATED) {
                return false;
            }
            int n = a.length;
            @SuppressWarnings("unchecked")
            CacheEnumerator<T>[] b = new CacheEnumerator[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = inner;
            if (enumerators.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(CacheEnumerator<T> inner) {
        for (;;) {
            CacheEnumerator<T>[] a = enumerators.get();
            int n = a.length;
            if (n == 0) {
                break;
            }
            int j = -1;
            for (int i = 0; i < n; i++) {
                if (a[i] == inner) {
                    j = i;
                    break;
                }
            }

            if (j < 0) {
                break;
            }
            CacheEnumerator<T>[] b;
            if (n == 1) {
                b = EMPTY;
            } else {
                b = new CacheEnumerator[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (enumerators.compareAndSet(a, b)) {
                break;
            }
        }
    }

    void signal(CacheEnumerator<T> target) {
        if (target.getAndIncrement() == 0) {
            do {
                CompletableFuture<Boolean> cf = target.completable;
                if (cf != null) {
                    int index = target.index;

                    boolean d = done;
                    int s = size;
                    boolean empty = s == index;

                    if (d && empty) {
                        target.completable = null;
                        Throwable ex = error;
                        if (ex != null) {
                            cf.completeExceptionally(ex);
                        } else {
                            cf.complete(false);
                        }
                    }

                    if (!empty) {
                        target.result = list.get(index);
                        target.index = index + 1;
                        target.completable = null;
                        cf.complete(true);
                    }
                }
            } while (target.decrementAndGet() != 0);
        }
    }

    static final class CacheEnumerator<T> extends AtomicInteger implements AsyncEnumerator<T> {

        final AsyncCache<T> parent;

        volatile CompletableFuture<Boolean> completable;

        int index;

        T result;

        CacheEnumerator(AsyncCache<T> parent) {
            this.parent = parent;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            result = null;
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            parent.signal(this);
            return cf;
        }

        @Override
        public T current() {
            return result;
        }

        @Override
        public void cancel() {
            parent.remove(this);
        }
    }
}

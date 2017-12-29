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

final class AsyncGroupBy<T, K, V> implements AsyncEnumerable<GroupedAsyncEnumerable<V, K>> {

    final AsyncEnumerable<T> source;

    final Function<? super T, ? extends K> keySelector;

    final Function<? super T, ? extends V> valueSelector;

    AsyncGroupBy(AsyncEnumerable<T> source, Function<? super T, ? extends K> keySelector, Function<? super T, ? extends V> valueSelector) {
        this.source = source;
        this.keySelector = keySelector;
        this.valueSelector = valueSelector;
    }

    @Override
    public AsyncEnumerator<GroupedAsyncEnumerable<V, K>> enumerator() {
        return new GroupByEnumerator<>(source.enumerator(), keySelector, valueSelector);
    }

    static final class GroupByEnumerator<T, K, V>
            implements AsyncEnumerator<GroupedAsyncEnumerable<V, K>>,
            BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Function<? super T, ? extends K> keySelector;

        final Function<? super T, ? extends V> valueSelector;

        final AtomicInteger sourceWip;

        final AtomicInteger wip;

        final AtomicInteger dispatchWip;

        final ConcurrentMap<K, GroupedEnumerator<T, K, V>> groups;

        final AtomicBoolean cancelled;

        final AtomicInteger active;

        volatile CompletableFuture<Boolean> completable;

        volatile GroupedAsyncEnumerable<V, K> current;
        volatile boolean done;
        volatile Throwable error;

        GroupByEnumerator(AsyncEnumerator<T> source, Function<? super T, ? extends K> keySelector, Function<? super T, ? extends V> valueSelector) {
            this.source = source;
            this.keySelector = keySelector;
            this.valueSelector = valueSelector;
            this.sourceWip = new AtomicInteger();
            this.wip = new AtomicInteger();
            this.dispatchWip = new AtomicInteger();
            this.groups = new ConcurrentHashMap<>();
            this.cancelled = new AtomicBoolean();
            this.active = new AtomicInteger(1);
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            current = null;
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            consumersReady();
            drain();
            return cf;
        }

        @Override
        public GroupedAsyncEnumerable<V, K> current() {
            return current;
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                if (active.decrementAndGet() == 0) {
                    source.cancel();
                } else {
                    consumersReady();
                }
            }
        }

        void remove(K key) {
            groups.remove(key);
            if (active.decrementAndGet() == 0) {
                source.cancel();
            } else {
                consumersReady();
            }
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                for (GroupedEnumerator<T, K, V> gr : groups.values()) {
                    gr.error = throwable;
                    gr.done = true;
                    gr.drain();
                }
                groups.clear();
                error = throwable;
                drain();
            } else
            if (aBoolean) {
                T v = source.current();
                K key = keySelector.apply(v);
                V value = valueSelector.apply(v);

                boolean isNew = false;
                GroupedEnumerator<T, K, V> gr = groups.get(key);
                if (gr == null) {
                    if (cancelled.get()) {
                        consumersReady();
                        return;
                    }

                    active.getAndIncrement();

                    gr = new GroupedEnumerator<>(key, this);
                    groups.put(key, gr);
                    isNew = true;
                }

                if (isNew) {
                    current = gr;
                    drain();
                }
                gr.result = value;
                gr.hasValue = true;
                gr.drain();
            } else {
                for (GroupedEnumerator<T, K, V> gr : groups.values()) {
                    gr.done = true;
                    gr.drain();
                }
                groups.clear();
                done = true;
                drain();
            }
        }

        void nextSource() {
            if (sourceWip.getAndIncrement() == 0) {
                do {
                    source.moveNext().whenComplete(this);
                } while (sourceWip.decrementAndGet() != 0);
            }
        }

        void consumersReady() {
            if (dispatchWip.getAndIncrement() == 0) {
                do {
                    if (completable != null || cancelled.get()) {
                        int s = 0;
                        int r = 0;
                        for (GroupedEnumerator<T, K, V> gr : groups.values()) {
                            if (gr.nonFirst && gr.completable != null) {
                                r++;
                            }
                            s++;
                        }
                        if (s == r) {
                            nextSource();
                        }
                    }
                } while (dispatchWip.decrementAndGet() != 0);
            }
        }

        void drain() {
            if (wip.getAndIncrement() == 0) {
                do {
                    CompletableFuture<Boolean> cf = completable;
                    if (cf != null) {
                        completable = null;
                        Throwable ex = error;
                        if (ex != null) {
                            cf.completeExceptionally(ex);
                            return;
                        }
                        if (done) {
                            cf.complete(false);
                        } else {
                            cf.complete(true);
                        }
                    }
                } while (wip.decrementAndGet() != 0);
            }
        }

        static final class GroupedEnumerator<T, K, V> implements GroupedAsyncEnumerable<V, K>, AsyncEnumerator<V> {

            final K key;

            final GroupByEnumerator<T, K, V> parent;

            final AtomicBoolean once;

            final AtomicBoolean cancelled;

            final AtomicInteger wip;

            volatile CompletableFuture<Boolean> completable;

            V result;
            volatile boolean hasValue;
            volatile boolean done;
            Throwable error;

            volatile boolean nonFirst;

            GroupedEnumerator(K key, GroupByEnumerator<T, K, V> parent) {
                this.key = key;
                this.parent = parent;
                this.once = new AtomicBoolean();
                this.wip = new AtomicInteger();
                this.cancelled = new AtomicBoolean();
            }

            @Override
            public K key() {
                return key;
            }

            @Override
            public AsyncEnumerator<V> enumerator() {
                if (once.compareAndSet(false, true)) {
                    return this;
                }
                return new AsyncError<V>(new IllegalStateException("Only one AsyncEnumerator allowed"));
            }

            @Override
            public CompletionStage<Boolean> moveNext() {
                CompletableFuture<Boolean> cf = new CompletableFuture<>();
                completable = cf;
                if (nonFirst) {
                    result = null;
                    hasValue = false;
                    parent.consumersReady();
                } else {
                    nonFirst = true;
                }
                drain();
                return cf;
            }

            @Override
            public V current() {
                return result;
            }

            void drain() {
                if (wip.getAndIncrement() == 0) {
                    do {
                        CompletableFuture<Boolean> cf = completable;
                        if (cf != null) {
                            if (done) {
                                Throwable ex = error;
                                if (ex == null) {
                                    cf.complete(false);
                                } else {
                                    cf.completeExceptionally(ex);
                                }
                                return;
                            }

                            if (hasValue) {
                                completable = null;
                                cf.complete(true);
                            }
                        }
                    } while (wip.decrementAndGet() != 0);
                }
            }

            @Override
            public void cancel() {
                if (cancelled.compareAndSet(false, true)) {
                    parent.remove(key);
                }
            }
        }
    }
}

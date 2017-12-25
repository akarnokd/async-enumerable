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

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

final class AsyncFromFlowPublisher<T> implements AsyncEnumerable<T> {

    final Flow.Publisher<T> source;

    AsyncFromFlowPublisher(Flow.Publisher<T> source) {
        this.source = source;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        FromFlowPublisherEnumerator<T> subscriber = new FromFlowPublisherEnumerator<>();
        source.subscribe(subscriber);
        return subscriber;
    }

    static final class FromFlowPublisherEnumerator<T>
            extends AtomicInteger
            implements AsyncEnumerator<T>, Flow.Subscriber<T> {

        final AtomicReference<Flow.Subscription> upstream;

        final AtomicLong requested;

        volatile T item;
        volatile boolean done;
        Throwable error;

        T current;

        volatile CompletableFuture<Boolean> completable;

        FromFlowPublisherEnumerator() {
            upstream = new AtomicReference<>();
            requested = new AtomicLong();
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            current = null;
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            deferredRequestOne();
            drain();
            return cf;
        }

        @Override
        public T current() {
            return current;
        }

        void deferredRequestOne() {
            Flow.Subscription current = upstream.get();
            if (current != null) {
                current.request(1);
            } else {
                for (;;) {
                    long r = requested.get();
                    long u = r + 1;
                    if (u < 0L) {
                        u = Long.MAX_VALUE;
                    }
                    if (requested.compareAndSet(r, u)) {
                        current = upstream.get();
                        if (current != null) {
                            u = requested.getAndSet(0L);
                            if (u != 0L) {
                                current.request(u);
                            }
                        }
                        break;
                    }
                }
            }
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            Objects.requireNonNull(subscription, "subscription == null");
            if (upstream.compareAndSet(null, subscription)) {
                long r = requested.getAndSet(0L);
                if (r != 0L) {
                    subscription.request(r);
                }
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onNext(T item) {
            this.item = item;
            drain();
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        void drain() {
            if (getAndIncrement() == 0) {
                do {
                    CompletableFuture<Boolean> cf = completable;
                    if (cf != null) {
                        boolean d = done;
                        T v = item;
                        if (d && v == null) {
                            completable = null;
                            Throwable ex = error;
                            if (ex == null) {
                                cf.complete(false);
                            } else {
                                cf.completeExceptionally(ex);
                            }
                            return;
                        }

                        if (v != null) {
                            current = item;
                            item = null;
                            completable = null;
                            cf.complete(true);
                        }
                    }
                } while (decrementAndGet() != 0);
            }
        }
    }
}

package hu.akarnokd.asyncenum;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.*;
import java.util.function.BiConsumer;

final class AsyncToFlowPublisher<T> implements Flow.Publisher<T> {

    final AsyncEnumerable<T> source;

    AsyncToFlowPublisher(AsyncEnumerable<T> source) {
        this.source = source;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        ToFlowPublisherSubscription<T> parent = new ToFlowPublisherSubscription<>(subscriber, source.enumerator());
        subscriber.onSubscribe(parent);
        parent.moveNext();
    }

    static final class ToFlowPublisherSubscription<T>
            extends AtomicInteger
            implements Flow.Subscription, BiConsumer<Boolean, Throwable> {

        final Flow.Subscriber<? super T> actual;

        final AsyncEnumerator<T> enumerator;

        final AtomicInteger mainWip;

        final AtomicLong requested;

        volatile boolean cancelled;

        volatile T item;
        volatile boolean done;
        Throwable error;

        long emitted;

        ToFlowPublisherSubscription(Flow.Subscriber<? super T> actual, AsyncEnumerator<T> enumerator) {
            this.actual = actual;
            this.enumerator = enumerator;
            this.requested = new AtomicLong();
            this.mainWip = new AtomicInteger();
        }

        @Override
        public void request(long n) {
            if (n <= 0L) {
                n = 1;
                // TODO cancel enumerator
                item = null;
                error = new IllegalArgumentException("§3.9 violated: positive request required");
                done = true;
            }
            for (;;) {
                long r = requested.get();
                long u = r + n;
                if (u < 0) {
                    u = Long.MAX_VALUE;
                }
                if (requested.compareAndSet(r, u)) {
                    drain();
                    break;
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            // TODO cancel enumerator
            if (getAndIncrement() == 0) {
                item = null;
            }
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                error = throwable;
                done = true;
            } else
            if (aBoolean) {
                item = enumerator.current();
            } else {
                done = true;
            }
            drain();
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            long e = emitted;
            int missed = 1;
            for (;;) {

                long r = requested.get();

                while (e != r) {
                    if (cancelled) {
                        item = null;
                        return;
                    }
                    boolean d = done;
                    T v = item;
                    item = null;
                    boolean empty = v == null;

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex == null) {
                            actual.onComplete();
                        } else {
                            actual.onError(ex);
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    actual.onNext(v);

                    e++;
                    moveNext();
                }

                if (e == r) {
                    if (cancelled) {
                        item = null;
                        return;
                    }
                    boolean d = done;

                    if (d && item == null) {
                        Throwable ex = error;
                        if (ex == null) {
                            actual.onComplete();
                        } else {
                            actual.onError(ex);
                        }
                        return;
                    }
                }

                emitted = e;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        void moveNext() {
            if (mainWip.getAndIncrement() == 0) {
                do {
                    enumerator.moveNext().whenComplete(this);
                } while (mainWip.decrementAndGet() != 0);
            }
        }
    }
}

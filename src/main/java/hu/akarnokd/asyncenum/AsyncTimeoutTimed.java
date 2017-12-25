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

        AsyncEnumerator<T> source;

        volatile CompletableFuture<Boolean> completable;

        Future<?> future;

        T result;

        TimeoutTimedEnumerator(AsyncEnumerator<T> source, long timeout, TimeUnit unit, ScheduledExecutorService executor, AsyncEnumerable<T> fallback) {
            this.source = source;
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
            AsyncEnumerator<T> en = source;
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
                result = source.current();
                cf.complete(true);
            } else {
                cf.complete(false);
            }
        }

        void timeout(long index) {
            if (this.index.compareAndSet(index, Long.MAX_VALUE)) {
                if (fallback != null) {
                    source = fallback.enumerator();
                    source.moveNext().whenComplete(this::acceptFallback);
                } else {
                    completable.completeExceptionally(new TimeoutException());
                }
            }
        }
    }
}

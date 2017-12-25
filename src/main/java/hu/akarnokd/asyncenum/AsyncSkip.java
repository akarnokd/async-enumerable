package hu.akarnokd.asyncenum;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

final class AsyncSkip<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> upstream;

    final long n;

    AsyncSkip(AsyncEnumerable<T> upstream, long n) {
        this.upstream = upstream;
        this.n = n;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new SkipEnumerator<>(upstream.enumerator(), n);
    }

    static final class SkipEnumerator<T> extends AtomicInteger
    implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        long n;

        CompletableFuture<Boolean> cf;

        SkipEnumerator(AsyncEnumerator<T> source, long n) {
            this.source = source;
            this.n = n + 1;
            this.cf = new CompletableFuture<>();
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (n > 0L) {
                CompletableFuture<Boolean> nx = cf;
                if (getAndIncrement() == 0) {
                    do {
                        source.moveNext().whenComplete(this);
                    } while (decrementAndGet() != 0);
                }
                return nx;
            }
            return source.moveNext();
        }

        @Override
        public T current() {
            return source.current();
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            CompletableFuture<Boolean> nx = cf;
            if (throwable != null) {
                cf = null;
                nx.completeExceptionally(throwable);
                return;
            }
            if (aBoolean) {
                if (--n <= 0L) {
                    cf = null;
                    nx.complete(true);
                } else {
                    moveNext();
                }
            } else {
                cf = null;
                nx.complete(false);
            }
        }
    }
}

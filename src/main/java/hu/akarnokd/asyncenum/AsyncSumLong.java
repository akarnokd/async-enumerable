package hu.akarnokd.asyncenum;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

final class AsyncSumLong<T> implements AsyncEnumerable<Long> {

    final AsyncEnumerable<T> source;

    final Function<? super T, ? extends Number> selector;

    AsyncSumLong(AsyncEnumerable<T> source, Function<? super T, ? extends Number> selector) {
        this.source = source;
        this.selector = selector;
    }

    @Override
    public AsyncEnumerator<Long> enumerator() {
        return new SumLongEnumerator<>(source.enumerator(), selector);
    }

    static final class SumLongEnumerator<T> extends AtomicInteger
    implements AsyncEnumerator<Long>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Function<? super T, ? extends Number> selector;

        boolean hasValue;
        long sum;

        Long result;
        boolean done;

        CompletableFuture<Boolean> cf;

        SumLongEnumerator(AsyncEnumerator<T> source, Function<? super T, ? extends Number> selector) {
            this.source = source;
            this.selector = selector;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (done) {
                result = null;
                return FALSE;
            }
            cf = new CompletableFuture<>();
            collectSource();
            return cf;
        }

        @Override
        public Long current() {
            return result;
        }

        void collectSource() {
            if (getAndIncrement() == 0) {
                do {
                    source.moveNext().whenComplete(this);
                } while (decrementAndGet() != 0);
            }
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                done = true;
                cf.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                sum += selector.apply(source.current()).longValue();
                hasValue = true;
                collectSource();
            } else {
                done = true;
                if (hasValue) {
                    result = sum;
                    cf.complete(true);
                } else {
                    cf.complete(false);
                }
            }
        }
    }
}

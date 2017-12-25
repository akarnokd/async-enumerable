package hu.akarnokd.asyncenum;

import java.util.Comparator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

final class AsyncMax<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Comparator<? super T> comparator;

    AsyncMax(AsyncEnumerable<T> source, Comparator<? super T> comparator) {
        this.source = source;
        this.comparator = comparator;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new SumLongEnumerator<>(source.enumerator(), comparator);
    }

    static final class SumLongEnumerator<T> extends AtomicInteger
    implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Comparator<? super T> comparator;

        boolean hasValue;
        T max;

        T result;
        boolean done;

        CompletableFuture<Boolean> cf;

        SumLongEnumerator(AsyncEnumerator<T> source, Comparator<? super T> comparator) {
            this.source = source;
            this.comparator = comparator;
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
        public T current() {
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
                if (hasValue) {
                    T curr = source.current();
                    if (comparator.compare(max, curr) <= 0) {
                        max = curr;
                    }
                } else {
                    hasValue = true;
                    max = source.current();
                }
                collectSource();
            } else {
                done = true;
                if (hasValue) {
                    result = max;
                    cf.complete(true);
                } else {
                    cf.complete(false);
                }
            }
        }
    }
}

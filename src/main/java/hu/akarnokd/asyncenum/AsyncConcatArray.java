package hu.akarnokd.asyncenum;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

final class AsyncConcatArray<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T>[] sources;

    AsyncConcatArray(AsyncEnumerable<T>[] sources) {
        this.sources = sources;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new ConcatArrayEnumerator<>(sources);
    }

    static final class ConcatArrayEnumerator<T> extends AtomicInteger
            implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerable<T>[] sources;

        AsyncEnumerator<T> currentEnumerator;

        CompletableFuture<Boolean> currentStage;

        int index;

        ConcatArrayEnumerator(AsyncEnumerable<T>[] sources) {
            this.sources = sources;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (currentEnumerator == null) {
                if (index == sources.length) {
                    return FALSE;
                }
                currentEnumerator = sources[index++].enumerator();
            }

            currentStage = new CompletableFuture<>();
            currentEnumerator.moveNext().whenComplete(this);
            return currentStage;
        }

        @Override
        public T current() {
            return currentEnumerator.current();
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                currentStage.completeExceptionally(throwable);
                return;
            }
            if (aBoolean) {
                currentStage.complete(true);
            } else {
                if (getAndIncrement() == 0) {
                    do {
                        if (index == sources.length) {
                            currentStage.complete(false);
                            break;
                        }
                        currentEnumerator = sources[index++].enumerator();
                        currentEnumerator.moveNext().whenComplete(this);
                    } while (decrementAndGet() != 0);
                }
            }
        }
    }
}

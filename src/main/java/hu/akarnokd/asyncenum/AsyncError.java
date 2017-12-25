package hu.akarnokd.asyncenum;

import java.util.concurrent.*;

final class AsyncError<T> implements AsyncEnumerable<T>, AsyncEnumerator<T> {

    final Throwable error;

    AsyncError(Throwable error) {
        this.error = error;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return this;
    }

    @Override
    public CompletionStage<Boolean> moveNext() {
        return CompletableFuture.failedStage(error);
    }

    @Override
    public T current() {
        return null;
    }
}

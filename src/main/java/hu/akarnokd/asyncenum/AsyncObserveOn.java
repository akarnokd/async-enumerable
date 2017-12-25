package hu.akarnokd.asyncenum;

import java.util.concurrent.*;

final class AsyncObserveOn<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Executor executor;

    AsyncObserveOn(AsyncEnumerable<T> source, Executor executor) {
        this.source = source;
        this.executor = executor;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new ObserveOnEnumerator<>(source.enumerator(), executor);
    }

    static final class ObserveOnEnumerator<T> implements AsyncEnumerator<T> {

        final AsyncEnumerator<T> source;

        final Executor executor;

        ObserveOnEnumerator(AsyncEnumerator<T> source, Executor executor) {
            this.source = source;
            this.executor = executor;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            AsyncCompletableFuture<Boolean> cf = new AsyncCompletableFuture<>();
            source.moveNext().whenCompleteAsync(cf, executor);
            return cf;
        }

        @Override
        public T current() {
            return source.current();
        }

    }

}

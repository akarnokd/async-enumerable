package hu.akarnokd.asyncenum;

import java.util.concurrent.*;

final class AsyncSubscribeOn<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Executor executor;

    AsyncSubscribeOn(AsyncEnumerable<T> source, Executor executor) {
        this.source = source;
        this.executor = executor;
    }


    @Override
    public AsyncEnumerator<T> enumerator() {
        SubscribeOnEnumerator<T> en = new SubscribeOnEnumerator<>(source);
        executor.execute(en);
        return en;
    }

    static final class SubscribeOnEnumerator<T> implements AsyncEnumerator<T>, Runnable {

        final CompletableFuture<AsyncEnumerator<T>> source;

        final AsyncEnumerable<T> upstream;

        SubscribeOnEnumerator(AsyncEnumerable<T> upstream) {
            this.upstream = upstream;
            this.source = new CompletableFuture<>();
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            AsyncEnumerator<T> en = source.getNow(null);
            if (en != null) {
                return en.moveNext();
            }
            return source.thenCompose(AsyncEnumerator::moveNext);
        }

        @Override
        public T current() {
            AsyncEnumerator<T> en = source.getNow(null);
            return en != null ? en.current() : null;
        }

        @Override
        public void run() {
            source.complete(upstream.enumerator());
        }
    }
}

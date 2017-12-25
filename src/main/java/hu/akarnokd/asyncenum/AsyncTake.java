package hu.akarnokd.asyncenum;

import java.util.concurrent.CompletionStage;

final class AsyncTake<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> upstream;

    final long n;

    AsyncTake(AsyncEnumerable<T> upstream, long n) {
        this.upstream = upstream;
        this.n = n;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new TakeEnumerator<>(upstream.enumerator(), n);
    }

    static final class TakeEnumerator<T> implements AsyncEnumerator<T> {

        final AsyncEnumerator<T> source;

        long n;

        TakeEnumerator(AsyncEnumerator<T> source, long n) {
            this.source = source;
            this.n = n;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (n-- <= 0L) {
                // TODO source.cancel()
                return FALSE;
            }
            return source.moveNext();
        }

        @Override
        public T current() {
            return source.current();
        }
    }
}

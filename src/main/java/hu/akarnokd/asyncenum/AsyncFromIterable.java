package hu.akarnokd.asyncenum;

import java.util.Iterator;
import java.util.concurrent.CompletionStage;

final class AsyncFromIterable<T> implements AsyncEnumerable<T> {

    final Iterable<T> iterable;

    AsyncFromIterable(Iterable<T> iterable) {
        this.iterable = iterable;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new FromIteratorEnumerator<>(iterable.iterator());
    }

    static final class FromIteratorEnumerator<T> implements AsyncEnumerator<T> {

        final Iterator<T> iterator;

        T current;

        FromIteratorEnumerator(Iterator<T> iterable) {
            this.iterator = iterable;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (iterator.hasNext()) {
                current = iterator.next();
                return TRUE;
            }
            current = null;
            return FALSE;
        }

        @Override
        public T current() {
            return current;
        }
    }
}

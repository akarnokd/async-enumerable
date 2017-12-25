package hu.akarnokd.asyncenum;

import java.util.concurrent.CompletionStage;

final class AsyncFromArray<T> implements AsyncEnumerable<T> {

    final T[] array;

    AsyncFromArray(T[] array) {
        this.array = array;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new FromArrayEnumerator<>(array);
    }

    static final class FromArrayEnumerator<T> implements AsyncEnumerator<T> {

        final T[] array;

        int index;

        T current;

        FromArrayEnumerator(T[] array) {
            this.array = array;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            int idx = index;
            if (idx == array.length) {
                current = null;
                return FALSE;
            }
            current = array[idx];
            index = idx + 1;
            return TRUE;
        }

        @Override
        public T current() {
            return current;
        }
    }
}

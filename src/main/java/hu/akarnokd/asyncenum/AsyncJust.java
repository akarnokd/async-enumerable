package hu.akarnokd.asyncenum;

import java.util.concurrent.CompletionStage;

final class AsyncJust<T> implements AsyncEnumerable<T> {

    final T value;

    AsyncJust(T value) {
        this.value = value;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new JustEnumerator<>(value);
    }

    static final class JustEnumerator<T> implements AsyncEnumerator<T> {

        final T value;

        boolean once;

        JustEnumerator(T value) {
            this.value = value;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (once) {
                return FALSE;
            }
            once = true;
            return TRUE;
        }

        @Override
        public T current() {
            return value;
        }
    }
}

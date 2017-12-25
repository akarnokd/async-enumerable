package hu.akarnokd.asyncenum;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

final class AsyncFromCompletionStage<T> implements AsyncEnumerable<T> {

    final CompletionStage<T> source;

    AsyncFromCompletionStage(CompletionStage<T> source) {
        this.source = source;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new FromCompletionStageAsyncEnumerable<>(source);
    }

    static final class FromCompletionStageAsyncEnumerable<T>
            implements AsyncEnumerator<T>, Function<T, Boolean> {

        final CompletionStage<T> stage;

        boolean once;

        T result;

        FromCompletionStageAsyncEnumerable(CompletionStage<T> stage) {
            this.stage = stage;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (once) {
                result = null;
                return FALSE;
            }
            once = true;
            return stage.thenApply(this);
        }

        @Override
        public T current() {
            return result;
        }

        @Override
        public Boolean apply(T t) {
            result = t;
            return true;
        }
    }
}

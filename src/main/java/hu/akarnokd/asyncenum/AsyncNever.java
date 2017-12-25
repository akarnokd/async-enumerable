package hu.akarnokd.asyncenum;

import java.util.concurrent.*;

enum AsyncNever implements AsyncEnumerable<Object>, AsyncEnumerator<Object> {

    INSTANCE;

    @SuppressWarnings("unchecked")
    public static <T> AsyncEnumerable<T> instance() {
        return (AsyncEnumerable<T>)INSTANCE;
    }

    @Override
    public AsyncEnumerator<Object> enumerator() {
        return this;
    }

    @Override
    public CompletionStage<Boolean> moveNext() {
        return new CompletableFuture<>();
    }

    @Override
    public Object current() {
        return null;
    }
}

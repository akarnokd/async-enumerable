package hu.akarnokd.asyncenum;

import java.util.concurrent.CompletionStage;

enum AsyncEmpty implements AsyncEnumerable<Object>, AsyncEnumerator<Object> {
    INSTANCE;

    @SuppressWarnings("unchecked")
    static <T> AsyncEnumerable<T> instance() {
        return (AsyncEnumerable<T>)INSTANCE;
    }

    @Override
    public AsyncEnumerator<Object> enumerator() {
        return this;
    }

    @Override
    public CompletionStage<Boolean> moveNext() {
        return FALSE;
    }

    @Override
    public Object current() {
        return null;
    }
}

package hu.akarnokd.asyncenum;

import java.util.function.Supplier;

final class AsyncDefer<T> implements AsyncEnumerable<T> {

    final Supplier<? extends AsyncEnumerable<? extends T>> supplier;

    AsyncDefer(Supplier<? extends AsyncEnumerable<? extends T>> supplier) {
        this.supplier = supplier;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AsyncEnumerator<T> enumerator() {
        return (AsyncEnumerator<T>)supplier.get().enumerator();
    }
}

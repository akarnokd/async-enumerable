package hu.akarnokd.asyncenum;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

enum CancelledAsyncEnumerator implements AsyncEnumerator<Object> {

    INSTANCE;

    @Override
    public CompletionStage<Boolean> moveNext() {
        return AsyncEnumerable.CANCELLED;
    }

    @Override
    public Object current() {
        return null;
    }

    @SuppressWarnings("unchecked")
    static <T> boolean cancel(AtomicReference<AsyncEnumerator<T>> target) {
        AsyncEnumerator<?> current = target.getAndSet((AsyncEnumerator<T>)INSTANCE);
        if (current != INSTANCE) {
            if (current != null) {
                current.cancel();
            }
            return true;
        }
        return false;
    }

    static <T> boolean replace(AtomicReference<AsyncEnumerator<T>> target, AsyncEnumerator<T> next) {
        for (;;) {
            AsyncEnumerator<T> current = target.getAcquire();
            if (current == INSTANCE) {
                next.cancel();
                return false;
            }
            if (target.compareAndSet(current, next)) {
                return true;
            }
        }
    }
}

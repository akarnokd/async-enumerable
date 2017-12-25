package hu.akarnokd.asyncenum;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

final class AsyncCompletableFuture<T> extends CompletableFuture<T>
implements BiConsumer<T, Throwable> {

    @Override
    public void accept(T t, Throwable throwable) {
        if (throwable != null) {
            completeExceptionally(throwable);
        } else {
            complete(t);
        }
    }
}

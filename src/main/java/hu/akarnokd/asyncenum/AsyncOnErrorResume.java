package hu.akarnokd.asyncenum;

import java.util.concurrent.*;
import java.util.function.*;

final class AsyncOnErrorResume<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Function<? super Throwable, ? extends AsyncEnumerable<? extends T>> resumeMapper;

    AsyncOnErrorResume(AsyncEnumerable<T> source, Function<? super Throwable, ? extends AsyncEnumerable<? extends T>> resumeMapper) {
        this.source = source;
        this.resumeMapper = resumeMapper;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new OnErrorResumeEnumerator<>(source.enumerator(), resumeMapper);
    }

    static final class OnErrorResumeEnumerator<T> implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final Function<? super Throwable, ? extends AsyncEnumerable<? extends T>> resumeMapper;

        AsyncEnumerator<T> source;

        T result;

        CompletableFuture<Boolean> completable;

        boolean inFallback;

        OnErrorResumeEnumerator(AsyncEnumerator<T> source, Function<? super Throwable, ? extends AsyncEnumerable<? extends T>> resumeMapper) {
            this.source = source;
            this.resumeMapper = resumeMapper;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            source.moveNext().whenComplete(this);
            return cf;
        }

        @Override
        public T current() {
            return result;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void accept(Boolean aBoolean, Throwable throwable) {
            CompletableFuture<Boolean> cf = completable;
            if (inFallback) {
                if (throwable != null) {
                    result = null;
                    cf.completeExceptionally(throwable);
                    return;
                }
            } else {
                if (throwable != null) {
                    inFallback = true;
                    result = null;
                    source = (AsyncEnumerator<T>)resumeMapper.apply(throwable).enumerator();
                    source.moveNext().whenComplete(this);
                    return;
                }
            }
            if (aBoolean) {
                result = source.current();
                cf.complete(true);
            } else {
                result = null;
                cf.complete(false);
            }
        }
    }
}

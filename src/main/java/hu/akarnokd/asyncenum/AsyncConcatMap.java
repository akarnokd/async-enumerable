package hu.akarnokd.asyncenum;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

final class AsyncConcatMap<T, R> implements AsyncEnumerable<R> {

    final AsyncEnumerable<T> source;

    final Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper;

    public AsyncConcatMap(AsyncEnumerable<T> source, Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public AsyncEnumerator<R> enumerator() {
        return new ConcatMapEnumerator<>(source.enumerator(), mapper);
    }

    static final class ConcatMapEnumerator<T, R>
            implements AsyncEnumerator<R>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper;

        final AtomicInteger wipMain;

        final AtomicInteger wipInner;

        AsyncEnumerator<? extends R> currentSource;

        volatile CompletableFuture<Boolean> completable;

        R current;

        ConcatMapEnumerator(AsyncEnumerator<T> source, Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper) {
            this.source = source;
            this.mapper = mapper;
            this.wipMain = new AtomicInteger();
            this.wipInner = new AtomicInteger();
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            if (currentSource == null) {
                nextMain();
            } else {
                nextInner();
            }
            return cf;
        }

        @Override
        public R current() {
            return current;
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                completable.completeExceptionally(throwable);
                return;
            }
            if (aBoolean) {
                current = currentSource.current();
                completable.complete(true);
            } else {
                currentSource = null;
                nextMain();
            }
        }

        public void acceptMain(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                completable.completeExceptionally(throwable);
                return;
            }
            if (aBoolean) {
                currentSource = mapper.apply(source.current()).enumerator();
                nextInner();
            } else {
                completable.complete(false);
            }
        }

        void nextMain() {
            if (wipMain.getAndIncrement() == 0) {
                do {
                    source.moveNext().whenComplete(this::acceptMain);
                } while (wipMain.decrementAndGet() != 0);
            }
        }

        void nextInner() {
            if (wipInner.getAndIncrement() == 0) {
                do {
                    currentSource.moveNext().whenComplete(this);
                } while (wipInner.decrementAndGet() != 0);
            }
        }
    }
}

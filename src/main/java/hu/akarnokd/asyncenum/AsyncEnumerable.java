package hu.akarnokd.asyncenum;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public interface AsyncEnumerable<T> {

    AsyncEnumerator<T> enumerator();

    CompletionStage<Boolean> TRUE = CompletableFuture.completedStage(true);

    CompletionStage<Boolean> FALSE = CompletableFuture.completedStage(false);

    CompletionStage<Boolean> CANCELLED = CompletableFuture.failedStage(new CancellationException() {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    });

    // -------------------------------------------------------------------------------------
    // Static factories

    static AsyncEnumerable<Integer> range(int start, int count) {
        return new AsyncRange(start, count);
    }

    static <T> AsyncEnumerable<T> empty() {
        return AsyncEmpty.instance();
    }

    @SafeVarargs
    static <T> AsyncEnumerable<T> fromArray(T... array) {
        return new AsyncFromArray<>(array);
    }

    static <T> AsyncEnumerable<T> fromIterable(Iterable<T> iterable) {
        return new AsyncFromIterable<>(iterable);
    }

    @SafeVarargs
    static <T> AsyncEnumerable<T> concat(AsyncEnumerable<T>... sources) {
        return new AsyncConcatArray<>(sources);
    }

    static AsyncEnumerable<Integer> characters(CharSequence chars) {
        return new AsyncFromCharSequence(chars);
    }

    static <T> AsyncEnumerable<T> fromCompletionStage(CompletionStage<T> stage) {
        return new AsyncFromCompletionStage<>(stage);
    }

    static AsyncEnumerable<Long> timer(long time, TimeUnit unit, ScheduledExecutorService executor) {
        return new AsyncTimer(time, unit, executor);
    }

    static <T> AsyncEnumerable<T> just(T item) {
        return new AsyncJust<>(item);
    }

    static <T> AsyncEnumerable<T> fromFlowPublisher(Flow.Publisher<T> source) {
        return new AsyncFromFlowPublisher<>(source);
    }

    static <T> AsyncEnumerable<T> never() {
        return AsyncNever.instance();
    }

    static <T> AsyncEnumerable<T> error(Throwable error) {
        return new AsyncError<>(error);
    }

    static <T> AsyncEnumerable<T> defer(Supplier<? extends AsyncEnumerable<? extends T>> supplier) {
        return new AsyncDefer<>(supplier);
    }

    // -------------------------------------------------------------------------------------
    // Instance transformations

    default <R> AsyncEnumerable<R> flatMap(
            Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper) {
        return new AsyncFlatMap<>(this, mapper);
    }

    default AsyncEnumerable<T> take(long n) {
        return new AsyncTake<>(this, n);
    }

    default AsyncEnumerable<T> skip(long n) {
        return new AsyncSkip<>(this, n);
    }

    default <R> AsyncEnumerable<R> map(Function<? super T, ? extends R> mapper) {
        return new AsyncMap<>(this, mapper);
    }

    default AsyncEnumerable<T> filter(Predicate<? super T> predicate) {
        return new AsyncFilter<>(this, predicate);
    }

    default <C> AsyncEnumerable<C> collect(Supplier<C> collection, BiConsumer<C, T> collector) {
        return new AsyncCollect<>(this, collection, collector);
    }

    default AsyncEnumerable<Long> sumLong(Function<? super T, ? extends Number> selector) {
        return new AsyncSumLong<>(this, selector);
    }

    default AsyncEnumerable<Integer> sumInt(Function<? super T, ? extends Number> selector) {
        return new AsyncSumInt<>(this, selector);
    }

    default AsyncEnumerable<T> max(Comparator<? super T> comparator) {
        return new AsyncMax<>(this, comparator);
    }

    default AsyncEnumerable<List<T>> toList() {
        return collect(ArrayList::new, List::add);
    }

    default AsyncEnumerable<T> subscribeOn(Executor executor) {
        return new AsyncSubscribeOn<>(this, executor);
    }

    default AsyncEnumerable<T> observeOn(Executor executor) {
        return new AsyncObserveOn<>(this, executor);
    }

    default <U> AsyncEnumerable<T> takeUntil(AsyncEnumerable<U> other) {
        return new AsyncTakeUntil<>(this, other);
    }

    default <R> AsyncEnumerable<R> concatMap(Function<? super T, ? extends AsyncEnumerable<? extends R>> mapper) {
        return new AsyncConcatMap<>(this, mapper);
    }

    default Flow.Publisher<T> toFlowPublisher() {
        return new AsyncToFlowPublisher<>(this);
    }

    default AsyncEnumerable<T> timeout(long timeout, TimeUnit unit, ScheduledExecutorService executor) {
        return new AsyncTimeoutTimed<>(this, timeout, unit, executor, null);
    }

    default AsyncEnumerable<T> timeout(long timeout, TimeUnit unit, ScheduledExecutorService executor, AsyncEnumerable<T> fallback) {
        return new AsyncTimeoutTimed<>(this, timeout, unit, executor,
                Objects.requireNonNull(fallback, "fallback == null"));
    }

    default AsyncEnumerable<T> onErrorResume(Function<? super Throwable, ? extends AsyncEnumerable<? extends T>> resumeMapper) {
        return new AsyncOnErrorResume<>(this, resumeMapper);
    }

    default <R> R to(Function<? super AsyncEnumerable<T>, R> converter) {
        return converter.apply(this);
    }

    default <R> AsyncEnumerable<R> compose(Function<? super AsyncEnumerable<T>, ? extends AsyncEnumerable<R>> composer) {
        return to(composer);
    }

    // -------------------------------------------------------------------------------------
    // Instance consumers

    default CompletionStage<Boolean> forEach(Consumer<? super T> consumer) {
        return AsyncForEach.forEach(enumerator(), consumer);
    }

    default T blockingFirst() {
        AsyncEnumerator<T> en = enumerator();
        try {
            Boolean result = en.moveNext().toCompletableFuture().get();
            if (result) {
                // TODO cancel rest
                return en.current();
            }
            throw new NoSuchElementException();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }
}

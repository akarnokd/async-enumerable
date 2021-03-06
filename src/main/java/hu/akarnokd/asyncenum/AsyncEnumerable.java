/*
 * Copyright 2017 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.asyncenum;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Represents an possibly asynchronous, cold-deferred, source of zero or more items
 * optionally followed by a Throwable.
 * @param <T> the value type.
 */
@FunctionalInterface
public interface AsyncEnumerable<T> {

    /**
     * Returns an AsyncEnumerator that can be iterated over to receive
     * the next item, the end-of-sequence indicator or a Throwable.
     * @return the new AsyncEnumerator instance
     */
    AsyncEnumerator<T> enumerator();

    /** Constant and already completed CompletionStage signalling {@code true}. */
    CompletionStage<Boolean> TRUE = CompletableFuture.completedStage(true);

    /** Constant and already completed CompletionStage signalling {@code false}. */
    CompletionStage<Boolean> FALSE = CompletableFuture.completedStage(false);

    CompletionStage<Boolean> CANCELLED = CompletableFuture.failedStage(new CancelledEnumeratorException());

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
    static <T> AsyncEnumerable<T> concatArray(AsyncEnumerable<T>... sources) {
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

    @SafeVarargs
    static <T, R> AsyncEnumerable<R> zipArray(
            Function<? super Object[], ? extends R> zipper, AsyncEnumerable<? extends T>... sources
    ) {
        return new AsyncZipArray<>(sources, zipper);
    }

    @SafeVarargs
    static <T> AsyncEnumerable<T> mergeArray(AsyncEnumerable<? extends T>... sources) {
        return fromArray(sources).flatMap(v -> v);
    }

    static AsyncEnumerable<Long> interval(long period, TimeUnit unit, ScheduledExecutorService executor) {
        return interval(period, period, unit, executor);
    }

    static AsyncEnumerable<Long> interval(long initialDelay, long period, TimeUnit unit, ScheduledExecutorService executor) {
        return new AsyncInterval(initialDelay, period, unit, executor);
    }

    static <T> AsyncEnumerable<T> fromCallable(Callable<? extends T> callable) {
        return new AsyncFromCallable<>(callable);
    }

    static <T> AsyncEnumerable<T> repeatItem(T item) {
        return new AsyncRepeatItem<>(item);
    }

    static <T> AsyncEnumerable<T> repeatCallable(Callable<? extends T> callable) {
        return new AsyncRepeatCallable<>(callable);
    }

    static <T> AsyncEnumerable<T> fromStream(Stream<T> stream) {
        return new AsyncFromStream<>(stream);
    }

    static <T> AsyncEnumerable<T> generate(Consumer<SyncEmitter<T>> generator) {
        return generate(() -> null, (s, e) -> { generator.accept(e); return s; }, s -> { });
    }

    static <T, S> AsyncEnumerable<T> generate(Supplier<S> state, BiFunction<S, SyncEmitter<T>, S> generator) {
        return generate(state, generator, s -> { });
    }


    static <T, S> AsyncEnumerable<T> generate(Supplier<S> state, BiFunction<S, SyncEmitter<T>, S> generator, Consumer<? super S> releaseState) {
        return new AsyncGenerate<>(state, generator, releaseState);
    }

    static <T, U> AsyncEnumerable<T> using(Supplier<U> resource, Function<? super U, ? extends AsyncEnumerable<T>> handler, Consumer<? super U> releaseResource) {
        return new AsyncUsing<>(resource, handler, releaseResource);
    }

    static <T> AsyncEnumerable<T> create(Consumer<AsyncEmitter<T>> emitter) {
        return new AsyncCreate<>(emitter);
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

    default AsyncEnumerable<T> min(Comparator<? super T> comparator) {
        return new AsyncMax<>(this, comparator.reversed());
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

    default AsyncEnumerable<T> concatWith(AsyncEnumerable<T> other) {
        return concatArray(this, other);
    }

    default AsyncEnumerable<T> startWith(AsyncEnumerable<T> other) {
        return concatArray(other, this);
    }

    @SuppressWarnings("unchecked")
    default <U, R> AsyncEnumerable<R> zipWith(AsyncEnumerable<U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        return zipArray(a -> zipper.apply((T)a[0], (U)a[1]), this, other);
    }

    default AsyncEnumerable<T> mergeWith(AsyncEnumerable<T> other) {
        return mergeArray(this, other);
    }

    default AsyncEnumerable<T> doOnNext(Consumer<? super T> onNext) {
        return new AsyncDoOn<>(this, onNext, t -> { }, () -> { });
    }

    default AsyncEnumerable<T> doOnError(Consumer<? super Throwable> onError) {
        return new AsyncDoOn<>(this, t -> { }, onError, () -> { });
    }

    default AsyncEnumerable<T> doOnComplete(Runnable onComplete) {
        return new AsyncDoOn<>(this, t -> { }, t -> { }, onComplete);
    }

    default AsyncEnumerable<T> doFinally(Runnable onFinally) {
        return new AsyncDoFinally<>(this, onFinally);
    }

    default AsyncEnumerable<T> ignoreElements() {
        return new AsyncIgnoreElements<>(this);
    }

    default AsyncEnumerable<T> doOnCancel(Runnable onCancel) {
        return new AsyncDoOnCancel<>(this, onCancel);
    }

    default AsyncEnumerable<T> repeat(long times) {
        return repeat(times, () -> false);
    }

    default AsyncEnumerable<T> repeat(BooleanSupplier stop) {
        return repeat(Long.MAX_VALUE, stop);
    }

    default AsyncEnumerable<T> repeat(long times, BooleanSupplier stop) {
        return new AsyncRepeat<>(this, times, stop);
    }

    default AsyncEnumerable<T> retry(long times) {
        return retry(times, e -> true);
    }

    default AsyncEnumerable<T> retry(Predicate<? super Throwable> predicate) {
        return retry(Long.MAX_VALUE, predicate);
    }

    default AsyncEnumerable<T> retry(long times, Predicate<? super Throwable> predicate) {
        return new AsyncRetry<>(this, times, predicate);
    }

    default AsyncEnumerable<T> repeatWhen(Supplier<? extends CompletionStage<Boolean>> completer) {
        return repeatWhen(() -> null, s -> completer.get());
    }

    default <S> AsyncEnumerable<T> repeatWhen(Supplier<S> stateSupplier, Function<? super S, ? extends CompletionStage<Boolean>> completer) {
        return new AsyncRepeatWhen<>(this, stateSupplier, completer);
    }

    default <S> AsyncEnumerable<T> retryWhen(Function<? super Throwable, ? extends CompletionStage<Boolean>> completer) {
        return retryWhen(() -> null, (s, e) -> completer.apply(e));
    }

    default <S> AsyncEnumerable<T> retryWhen(Supplier<S> stateSupplier, BiFunction<? super S, ? super Throwable, ? extends CompletionStage<Boolean>> completer) {
        return new AsyncRetryWhen<>(this, stateSupplier, completer);
    }

    default <K> AsyncEnumerable<GroupedAsyncEnumerable<T, K>> groupBy(Function<? super T, ? extends K> keySelector) {
        return groupBy(keySelector, v -> v);
    }

    default <K, V> AsyncEnumerable<GroupedAsyncEnumerable<V, K>> groupBy(Function<? super T, ? extends K> keySelector, Function<? super T, ? extends V> valueSelector) {
        return new AsyncGroupBy<>(this, keySelector, valueSelector);
    }

    default AsyncEnumerable<T> skipWhile(Predicate<? super T> predicate) {
        return new AsyncSkipWhile<>(this, predicate);
    }

    default AsyncEnumerable<T> takeWhile(Predicate<? super T> predicate) {
        return new AsyncTakeWhile<>(this, predicate);
    }

    default AsyncEnumerable<T> takeUntil(Predicate<? super T> stopPredicate) {
        return new AsyncTakeUntilPredicate<>(this, stopPredicate);
    }

    default <A, R> AsyncEnumerable<R> collect(Collector<T, A, R> collector) {
        return new AsyncCollectWith<>(this, collector);
    }

    default AsyncEnumerable<T> cache() {
        return new AsyncCache<>(this);
    }

    default AsyncEnumerable<T> distinct() {
        return distinct(v -> v, HashSet::new);
    }

    default <K> AsyncEnumerable<T> distinct(Function<? super T, ? extends K> keySelector) {
        return distinct(keySelector, HashSet::new);
    }

    default <K> AsyncEnumerable<T> distinct(Function<? super T, ? extends K> keySelector, Supplier<? extends Collection<? super K>> setSupplier) {
        return new AsyncDistinct<>(this, keySelector, setSupplier);
    }

    default AsyncEnumerable<T> distinctUntilChanged() {
        return distinctUntilChanged(v -> v, Objects::equals);
    }

    default <K> AsyncEnumerable<T> distinctUntilChanged(Function<? super T, ? extends K> keySelector) {
        return distinctUntilChanged(keySelector, Objects::equals);
    }

    default <K> AsyncEnumerable<T> distinctUntilChanged(Function<? super T, ? extends K> keySelector, BiPredicate<? super K, ? super K> comparer) {
        return new AsyncDistinctUntilChanged<>(this, keySelector, comparer);
    }

    default AsyncEnumerable<T> reduce(BiFunction<T, T, T> reducer) {
        return new AsyncReduce<>(this, reducer);
    }

    default <R> AsyncEnumerable<R> reduce(Supplier<R> initial, BiFunction<R, T, R> reducer) {
        return new AsyncReduceWith<>(this, initial, reducer);
    }

    default AsyncEnumerable<T> skipLast(int n) {
        if (n <= 0) {
            return this;
        }
        return new AsyncSkipLast<>(this, n);
    }

    default AsyncEnumerable<T> takeLast(int n) {
        if (n <= 0) {
            return ignoreElements();
        }
        return new AsyncTakeLast<>(this, n);
    }

    default <R> AsyncEnumerable<R> publish(Function<? super AsyncEnumerable<T>, ? extends AsyncEnumerable<R>> handler) {
        return new AsyncPublish<>(this, handler);
    }

    default AsyncEnumerable<T> switchIfEmpty(AsyncEnumerable<T> fallback) {
        return new AsyncSwitchIfEmpty<>(this, fallback);
    }

    default AsyncEnumerable<T> first() {
        return new AsyncFirst<>(this);
    }

    default AsyncEnumerable<T> last() {
        return new AsyncLast<>(this);
    }

    // -------------------------------------------------------------------------------------
    // Instance consumers

    default CompletionStage<Boolean> forEach(Consumer<? super T> consumer) {
        return AsyncForEach.forEach(enumerator(), consumer);
    }

    default T blockingFirst() {
        return AsyncBlockingFirst.blockingFirst(enumerator());
    }

    default T blockingLast() {
        AsyncBlockingLast<T> bl = new AsyncBlockingLast<>(enumerator());
        bl.moveNext();
        return bl.blockingGet();
    }

    default Iterable<T> blockingIterable() {
        return new AsyncBlockingIterable<>(this);
    }

    default Stream<T> blockingStream() {
        Iterator<T> it = blockingIterable().iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, 0), false).onClose((Runnable)it);
    }

    default Optional<T> blockingFirstOptional() {
        return AsyncBlockingFirst.blockingFirstOptional(enumerator());
    }

    default Optional<T> blockingLastOptional() {
        AsyncBlockingLast<T> bl = new AsyncBlockingLast<>(enumerator());
        bl.moveNext();
        return bl.blockingGetOptional();
    }
}

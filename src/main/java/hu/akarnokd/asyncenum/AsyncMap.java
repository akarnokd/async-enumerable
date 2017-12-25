package hu.akarnokd.asyncenum;

import java.util.concurrent.CompletionStage;
import java.util.function.*;

final class AsyncMap<T, R> implements AsyncEnumerable<R> {

    final AsyncEnumerable<T> source;

    final Function<? super T, ? extends R> mapper;

    AsyncMap(AsyncEnumerable<T> source, Function<? super T, ? extends R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public AsyncEnumerator<R> enumerator() {
        return new MapEnumerator<>(source.enumerator(), mapper);
    }

    static final class MapEnumerator<T, R> implements AsyncEnumerator<R> {

        final AsyncEnumerator<T> source;

        final Function<? super T, ? extends R> mapper;

        MapEnumerator(AsyncEnumerator<T> source, Function<? super T, ? extends R> mapper) {
            this.source = source;
            this.mapper = mapper;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            return source.moveNext();
        }

        @Override
        public R current() {
            return mapper.apply(source.current());
        }
    }
}

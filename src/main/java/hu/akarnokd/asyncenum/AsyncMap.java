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

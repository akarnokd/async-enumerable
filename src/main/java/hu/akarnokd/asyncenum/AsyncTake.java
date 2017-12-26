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

final class AsyncTake<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> upstream;

    final long n;

    AsyncTake(AsyncEnumerable<T> upstream, long n) {
        this.upstream = upstream;
        this.n = n;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new TakeEnumerator<>(upstream.enumerator(), n);
    }

    static final class TakeEnumerator<T> implements AsyncEnumerator<T> {

        final AsyncEnumerator<T> source;

        long n;

        TakeEnumerator(AsyncEnumerator<T> source, long n) {
            this.source = source;
            this.n = n;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (n-- <= 0L) {
                source.cancel();
                return FALSE;
            }
            return source.moveNext();
        }

        @Override
        public T current() {
            return source.current();
        }
    }
}

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

import java.util.Iterator;
import java.util.concurrent.CompletionStage;

final class AsyncFromIterable<T> implements AsyncEnumerable<T> {

    final Iterable<T> iterable;

    AsyncFromIterable(Iterable<T> iterable) {
        this.iterable = iterable;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new FromIteratorEnumerator<>(iterable.iterator());
    }

    static final class FromIteratorEnumerator<T> implements AsyncEnumerator<T> {

        final Iterator<T> iterator;

        T current;

        FromIteratorEnumerator(Iterator<T> iterable) {
            this.iterator = iterable;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (iterator.hasNext()) {
                current = iterator.next();
                return TRUE;
            }
            current = null;
            return FALSE;
        }

        @Override
        public T current() {
            return current;
        }
    }
}

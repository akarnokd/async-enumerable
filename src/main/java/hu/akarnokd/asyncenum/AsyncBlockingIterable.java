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
import java.util.concurrent.ExecutionException;

final class AsyncBlockingIterable<T> implements Iterable<T> {

    final AsyncEnumerable<T> source;

    AsyncBlockingIterable(AsyncEnumerable<T> source) {
        this.source = source;
    }

    @Override
    public Iterator<T> iterator() {
        return new BlockingIterator<>(source.enumerator());
    }

    static final class BlockingIterator<T> implements Iterator<T> {

        final AsyncEnumerator<T> source;

        boolean hasValue;

        boolean done;

        T value;

        BlockingIterator(AsyncEnumerator<T> source) {
            this.source = source;
        }

        @Override
        public boolean hasNext() {
            if (!hasValue && !done) {
                try {
                    Boolean b = source
                            .moveNext()
                            .toCompletableFuture()
                            .get();
                    if (b) {
                        hasValue = true;
                        value = source.current();
                    } else {
                        done = true;
                    }
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                } catch (ExecutionException ex) {
                    throw ThrowableHelper.wrapOrThrow(ex.getCause());
                }
            }
            return hasValue;
        }

        @Override
        public T next() {
            if (hasValue || hasNext()) {
                T v = value;
                value = null;
                hasValue = false;
                return v;
            }
            throw new NoSuchElementException();
        }
    }
}

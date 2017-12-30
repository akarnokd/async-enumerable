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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

final class AsyncFromStream<T> implements AsyncEnumerable<T> {

    final Stream<T> stream;

    AsyncFromStream(Stream<T> stream) {
        this.stream = stream;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        try {
            return new StreamEnumerator<>(stream.iterator(), stream);
        } catch (RuntimeException ex) {
            return new AsyncError<>(ex);
        }
    }

    static final class StreamEnumerator<T>
            extends AtomicBoolean
            implements AsyncEnumerator<T> {

        final Iterator<T> source;

        final Stream closeable;

        T current;

        StreamEnumerator(Iterator<T> source, Stream closeable) {
            this.source = source;
            this.closeable = closeable;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (source.hasNext()) {
                current = source.next();
                return TRUE;
            }
            current = null;
            cancel();
            return FALSE;
        }

        @Override
        public T current() {
            return current;
        }

        @Override
        public void cancel() {
            if (compareAndSet(false, true)) {
                closeable.close();
            }
        }
    }
}

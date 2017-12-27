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

final class AsyncDoOnCancel<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Runnable onCancel;

    AsyncDoOnCancel(AsyncEnumerable<T> source, Runnable onCancel) {
        this.source = source;
        this.onCancel = onCancel;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new DoOnCancelEnumerator<>(source.enumerator(), onCancel);
    }

    static final class DoOnCancelEnumerator<T> implements AsyncEnumerator<T> {

        final AsyncEnumerator<T> source;

        final Runnable onCancel;

        DoOnCancelEnumerator(AsyncEnumerator<T> source, Runnable onCancel) {
            this.source = source;
            this.onCancel = onCancel;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            return source.moveNext();
        }

        @Override
        public T current() {
            return source.current();
        }

        @Override
        public void cancel() {
            onCancel.run();
            source.cancel();
        }
    }
}

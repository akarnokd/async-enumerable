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

import java.util.concurrent.*;

final class AsyncFromCallable<T> implements AsyncEnumerable<T> {

    final Callable<? extends T> callable;

    AsyncFromCallable(Callable<? extends T> callable) {
        this.callable = callable;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new FromCallableEnumerator<>(callable);
    }

    static final class FromCallableEnumerator<T> implements AsyncEnumerator<T> {

        final Callable<? extends T> callable;

        T result;

        boolean once;

        FromCallableEnumerator(Callable<? extends T> callable) {
            this.callable = callable;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (once) {
                result = null;
                return FALSE;
            }
            once = true;
            try {
                result = callable.call();
            } catch (Exception ex) {
                return CompletableFuture.failedStage(ex);
            }
            return TRUE;
        }

        @Override
        public T current() {
            return result;
        }

        @Override
        public void cancel() {
            // No action, consumer should stop calling moveNext().
        }
    }
}

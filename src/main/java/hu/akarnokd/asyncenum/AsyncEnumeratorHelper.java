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
import java.util.concurrent.atomic.AtomicReference;

enum AsyncEnumeratorHelper implements AsyncEnumerator<Object> {

    CANCELLED;

    @Override
    public CompletionStage<Boolean> moveNext() {
        return AsyncEnumerable.CANCELLED;
    }

    @Override
    public Object current() {
        return null;
    }

    @SuppressWarnings("unchecked")
    static <T> boolean cancel(AtomicReference<AsyncEnumerator<T>> target) {
        AsyncEnumerator<?> current = target.getAndSet((AsyncEnumerator<T>)CANCELLED);
        if (current != CANCELLED) {
            if (current != null) {
                current.cancel();
            }
            return true;
        }
        return false;
    }

    static <T> boolean replace(AtomicReference<AsyncEnumerator<T>> target, AsyncEnumerator<T> next) {
        for (;;) {
            AsyncEnumerator<T> current = target.getAcquire();
            if (current == CANCELLED) {
                next.cancel();
                return false;
            }
            if (target.compareAndSet(current, next)) {
                return true;
            }
        }
    }
}

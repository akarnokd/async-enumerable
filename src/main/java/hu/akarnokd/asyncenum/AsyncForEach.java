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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

final class AsyncForEach {

    static <T> CompletionStage<Boolean> forEach(
            AsyncEnumerator<T> enumerator,
            Consumer<? super T> onValue) {
        CompletableFuture<Boolean> completion = new CompletableFuture<>();
        new ForEachTrampoline<>(completion, enumerator, onValue).moveNext();
        return completion;
    }

    static final class ForEachTrampoline<T> extends AtomicInteger implements BiConsumer<Boolean, Throwable> {
        final CompletableFuture<Boolean> completion;
        final AsyncEnumerator<T> enumerator;
        final Consumer<? super T> onValue;

        ForEachTrampoline(CompletableFuture<Boolean> completion, AsyncEnumerator<T> enumerator, Consumer<? super T> onValue) {
            this.completion = completion;
            this.enumerator = enumerator;
            this.onValue = onValue;
        }

        @Override
        public void accept(Boolean r, Throwable e) {
            if (e != null) {
                completion.completeExceptionally(e);
                return;
            }
            if (r) {
                onValue.accept(enumerator.current());
                moveNext();
            } else {
                completion.complete(true);
            }
        }

        void moveNext() {
            if (getAndIncrement() != 0) {
                return;
            }

            do {
                CompletionStage<Boolean> next = enumerator.moveNext();
                next.whenComplete(this);
            } while (decrementAndGet() != 0);
        }
    }
}

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

final class AsyncTakeUntil<T, U> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final AsyncEnumerable<U> other;

    AsyncTakeUntil(AsyncEnumerable<T> source, AsyncEnumerable<U> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        TakeUntilEnumerator<T, U> main = new TakeUntilEnumerator<>();
        other.enumerator().moveNext().whenComplete(main::acceptOther);
        main.source = source.enumerator();
        return main;
    }

    static final class TakeUntilEnumerator<T, U>
            extends AtomicReference<CompletableFuture<Boolean>>
            implements AsyncEnumerator<T>,
            BiConsumer<Boolean, Throwable> {

        AsyncEnumerator<T> source;

        CompletableFuture<Boolean> current;

        @Override
        public CompletionStage<Boolean> moveNext() {
            for (;;) {
                CompletableFuture<Boolean> curr = get();
                if (curr instanceof TerminalCompletableFuture) {
                    // TODO cancel the other
                    return curr;
                }
                CompletableFuture<Boolean> next = new CompletableFuture<>();
                if (compareAndSet(curr, next)) {
                    current = next;
                    source.moveNext().whenComplete(this);
                    return next;
                }
            }
        }

        @Override
        public T current() {
            return source.current();
        }


        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                // TODO cancel other
                current.completeExceptionally(throwable);
                return;
            }
            if (!aBoolean) {
                // TODO cancel other
            }
            current.complete(aBoolean);
        }

        public void acceptOther(Boolean aBoolean, Throwable throwable) {
            // TODO cancel source
            if (throwable == null) {
                CompletableFuture<Boolean> cf = getAndSet(STOP);
                if (cf != null && !(cf instanceof TerminalCompletableFuture)) {
                    cf.complete(false);
                }
            } else {
                TerminalCompletableFuture tf = new TerminalCompletableFuture();
                tf.completeExceptionally(throwable);
                CompletableFuture<Boolean> cf = getAndSet(tf);
                if (cf != null && !(cf instanceof TerminalCompletableFuture)) {
                    cf.completeExceptionally(throwable);
                }
            }
        }

        static final TerminalCompletableFuture STOP = new TerminalCompletableFuture();
        static {
            STOP.complete(false);
        }

        static final class TerminalCompletableFuture extends CompletableFuture<Boolean> {

        }
    }
}

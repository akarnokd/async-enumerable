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
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class AsyncRepeatWhen<T, S> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Supplier<S> stateSupplier;

    final Function<? super S, ? extends CompletionStage<Boolean>> completer;

    public AsyncRepeatWhen(AsyncEnumerable<T> source, Supplier<S> stateSupplier, Function<? super S, ? extends CompletionStage<Boolean>> completer) {
        this.source = source;
        this.stateSupplier = stateSupplier;
        this.completer = completer;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new RepeatWhenEnumerator<>(source, stateSupplier.get(), completer);
    }

    static final class RepeatWhenEnumerator<T, S>
    extends AtomicInteger
    implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerable<T> source;

        final S state;

        final Function<? super S, ? extends CompletionStage<Boolean>> completer;

        final AtomicReference<AsyncEnumerator<T>> current;

        T result;

        CompletableFuture<Boolean> completable;

        RepeatWhenEnumerator(AsyncEnumerable<T> source, S state, Function<? super S, ? extends CompletionStage<Boolean>> completer) {
            this.source = source;
            this.state = state;
            this.completer = completer;
            this.current = new AtomicReference<>(source.enumerator());
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            nextItem();
            return cf;
        }

        @Override
        public T current() {
            return result;
        }

        void nextItem() {
            if (getAndIncrement() == 0) {
                do {
                    current.get().moveNext().whenComplete(this);
                } while (decrementAndGet() != 0);
            }
        }

        @Override
        public void cancel() {
            AsyncEnumeratorHelper.cancel(current);
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                result = null;
                completable.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                result = current.getPlain().current();
                completable.complete(true);
            } else {
                result = null;
                completer.apply(state).whenComplete(this::acceptCompleter);
            }
        }

        void acceptCompleter(Boolean shouldRepeat, Throwable throwable) {
            if (throwable != null) {
                result = null;
                completable.completeExceptionally(throwable);
                return;
            }

            if (shouldRepeat) {
                if (AsyncEnumeratorHelper.replace(current, source.enumerator())) {
                    nextItem();
                }
            } else {
                completable.complete(false);
            }
        }
    }
}

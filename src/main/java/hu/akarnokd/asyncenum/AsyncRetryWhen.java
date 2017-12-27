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

public class AsyncRetryWhen<T, S> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Supplier<S> stateSupplier;

    final BiFunction<? super S, ? super Throwable, ? extends CompletionStage<Boolean>> completer;

    public AsyncRetryWhen(AsyncEnumerable<T> source, Supplier<S> stateSupplier, BiFunction<? super S, ? super Throwable, ? extends CompletionStage<Boolean>> completer) {
        this.source = source;
        this.stateSupplier = stateSupplier;
        this.completer = completer;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new RetryWhenEnumerator<>(source, stateSupplier.get(), completer);
    }

    static final class RetryWhenEnumerator<T, S>
    extends AtomicInteger
    implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerable<T> source;

        final S state;

        final BiFunction<? super S, ? super Throwable, ? extends CompletionStage<Boolean>> completer;

        final AtomicReference<AsyncEnumerator<T>> current;

        T result;

        CompletableFuture<Boolean> completable;

        RetryWhenEnumerator(AsyncEnumerable<T> source, S state, BiFunction<? super S, ? super Throwable, ? extends CompletionStage<Boolean>> completer) {
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
                completer.apply(state, throwable).whenComplete(this::acceptCompleter);
                return;
            }

            if (aBoolean) {
                result = current.getPlain().current();
                completable.complete(true);
            } else {
                result = null;
                completable.complete(false);
            }
        }

        void acceptCompleter(Boolean shouldRetry, Throwable throwable) {
            if (throwable != null) {
                result = null;
                completable.completeExceptionally(throwable);
                return;
            }

            if (shouldRetry) {
                if (AsyncEnumeratorHelper.replace(current, source.enumerator())) {
                    nextItem();
                }
            } else {
                completable.complete(false);
            }
        }
    }
}

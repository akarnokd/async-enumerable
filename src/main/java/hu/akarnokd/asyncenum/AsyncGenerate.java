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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

final class AsyncGenerate<T, S> implements AsyncEnumerable<T> {

    final Supplier<S> state;

    final BiFunction<S, SyncEmitter<T>, S> generator;

    final Consumer<? super S> releaseState;

    AsyncGenerate(Supplier<S> state, BiFunction<S, SyncEmitter<T>, S> generator, Consumer<? super S> releaseState) {
        this.state = state;
        this.generator = generator;
        this.releaseState = releaseState;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new GenerateEnumerator<>(state.get(), generator, releaseState);
    }

    static final class GenerateEnumerator<T, S>
            extends AtomicBoolean
            implements AsyncEnumerator<T>, SyncEmitter<T> {

        final BiFunction<S, SyncEmitter<T>, S> generator;

        final Consumer<? super S> releaseState;

        S state;

        T result;
        boolean hasValue;
        boolean done;
        Throwable error;

        GenerateEnumerator(S state, BiFunction<S, SyncEmitter<T>, S> generator, Consumer<? super S> releaseState) {
            this.state = state;
            this.generator = generator;
            this.releaseState = releaseState;
        }

        void cleanup() {
            if (compareAndSet(false, true)) {
                S s = state;
                state = null;
                releaseState.accept(s);
            }
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (error != null) {
                cleanup();
                return CompletableFuture.failedStage(error);
            }
            if (done) {
                cleanup();
                return FALSE;
            }
            hasValue = false;
            result = null;
            state = generator.apply(state, this);
            if (hasValue) {
                return TRUE;
            }
            cleanup();
            if (error != null) {
                return CompletableFuture.failedStage(error);
            }
            if (done) {
                return FALSE;
            }
            return CompletableFuture.failedStage(new IllegalStateException("None of the SyncEmitter methods were called in this round"));
        }

        @Override
        public T current() {
            return result;
        }

        @Override
        public void cancel() {
            cleanup();
        }

        @Override
        public void next(T item) {
            if (hasValue) {
                result = null;
                if (error == null) {
                    error = new IllegalStateException("next() called multiple times");
                } else {
                    error = new IllegalStateException("next() called multiple times", error);
                }
            } else {
                hasValue = true;
                result = item;
            }
        }

        @Override
        public void error(Throwable error) {
            if (this.error != null) {
                this.error = new IllegalStateException("error() called multiple times", error);
            } else {
                this.error = error;
            }
        }

        @Override
        public void stop() {
            done = true;
        }
    }
}

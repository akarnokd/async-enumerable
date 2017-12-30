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
import java.util.function.*;

final class AsyncTakeUntilPredicate<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> source;

    final Predicate<? super T> stopPredicate;

    AsyncTakeUntilPredicate(AsyncEnumerable<T> source, Predicate<? super T> stopPredicate) {
        this.source = source;
        this.stopPredicate = stopPredicate;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new TakeUntilPredicateEnumerator<>(source.enumerator(), stopPredicate);
    }

    static final class TakeUntilPredicateEnumerator<T> implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Predicate<? super T> stopPredicate;

        CompletableFuture<Boolean> completable;

        T current;

        boolean stop;

        TakeUntilPredicateEnumerator(AsyncEnumerator<T> source, Predicate<? super T> stopPredicate) {
            this.source = source;
            this.stopPredicate = stopPredicate;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            current = null;
            if (stop) {
                source.cancel();
                return FALSE;
            }
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            source.moveNext().whenComplete(this);
            return cf;
        }

        @Override
        public T current() {
            return current;
        }

        @Override
        public void cancel() {
            source.cancel();
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                completable.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                T v = source.current();
                current = v;
                if (stopPredicate.test(v)) {
                    stop = true;
                }
                completable.complete(true);
            } else {
                completable.complete(false);
            }
        }
    }
}

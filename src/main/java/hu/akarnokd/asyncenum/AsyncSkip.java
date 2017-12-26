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
import java.util.function.BiConsumer;

final class AsyncSkip<T> implements AsyncEnumerable<T> {

    final AsyncEnumerable<T> upstream;

    final long n;

    AsyncSkip(AsyncEnumerable<T> upstream, long n) {
        this.upstream = upstream;
        this.n = n;
    }

    @Override
    public AsyncEnumerator<T> enumerator() {
        return new SkipEnumerator<>(upstream.enumerator(), n);
    }

    static final class SkipEnumerator<T> extends AtomicInteger
    implements AsyncEnumerator<T>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        long n;

        CompletableFuture<Boolean> cf;

        SkipEnumerator(AsyncEnumerator<T> source, long n) {
            this.source = source;
            this.n = n + 1;
            this.cf = new CompletableFuture<>();
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (n > 0L) {
                CompletableFuture<Boolean> nx = cf;
                if (getAndIncrement() == 0) {
                    do {
                        source.moveNext().whenComplete(this);
                    } while (decrementAndGet() != 0);
                }
                return nx;
            }
            return source.moveNext();
        }

        @Override
        public T current() {
            return source.current();
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            CompletableFuture<Boolean> nx = cf;
            if (throwable != null) {
                cf = null;
                nx.completeExceptionally(throwable);
                return;
            }
            if (aBoolean) {
                if (--n <= 0L) {
                    cf = null;
                    nx.complete(true);
                } else {
                    moveNext();
                }
            } else {
                cf = null;
                nx.complete(false);
            }
        }

        @Override
        public void cancel() {
            source.cancel();
        }
    }
}

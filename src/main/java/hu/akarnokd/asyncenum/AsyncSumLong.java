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

final class AsyncSumLong<T> implements AsyncEnumerable<Long> {

    final AsyncEnumerable<T> source;

    final Function<? super T, ? extends Number> selector;

    AsyncSumLong(AsyncEnumerable<T> source, Function<? super T, ? extends Number> selector) {
        this.source = source;
        this.selector = selector;
    }

    @Override
    public AsyncEnumerator<Long> enumerator() {
        return new SumLongEnumerator<>(source.enumerator(), selector);
    }

    static final class SumLongEnumerator<T> extends AtomicInteger
    implements AsyncEnumerator<Long>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Function<? super T, ? extends Number> selector;

        boolean hasValue;
        long sum;

        Long result;
        boolean done;

        CompletableFuture<Boolean> cf;

        volatile boolean cancelled;

        SumLongEnumerator(AsyncEnumerator<T> source, Function<? super T, ? extends Number> selector) {
            this.source = source;
            this.selector = selector;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (done) {
                result = null;
                return FALSE;
            }
            cf = new CompletableFuture<>();
            collectSource();
            return cf;
        }

        @Override
        public Long current() {
            return result;
        }

        void collectSource() {
            if (getAndIncrement() == 0) {
                do {
                    if (cancelled) {
                        return;
                    }
                    source.moveNext().whenComplete(this);
                } while (decrementAndGet() != 0);
            }
        }

        @Override
        public void accept(Boolean aBoolean, Throwable throwable) {
            if (throwable != null) {
                done = true;
                cf.completeExceptionally(throwable);
                return;
            }

            if (aBoolean) {
                sum += selector.apply(source.current()).longValue();
                hasValue = true;
                collectSource();
            } else {
                done = true;
                if (hasValue) {
                    result = sum;
                    cf.complete(true);
                } else {
                    cf.complete(false);
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            source.cancel();
        }
    }
}

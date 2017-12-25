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

final class AsyncSumInt<T> implements AsyncEnumerable<Integer> {

    final AsyncEnumerable<T> source;

    final Function<? super T, ? extends Number> selector;

    AsyncSumInt(AsyncEnumerable<T> source, Function<? super T, ? extends Number> selector) {
        this.source = source;
        this.selector = selector;
    }

    @Override
    public AsyncEnumerator<Integer> enumerator() {
        return new SumIntEnumerator<>(source.enumerator(), selector);
    }

    static final class SumIntEnumerator<T> extends AtomicInteger
    implements AsyncEnumerator<Integer>, BiConsumer<Boolean, Throwable> {

        final AsyncEnumerator<T> source;

        final Function<? super T, ? extends Number> selector;

        boolean hasValue;
        int sum;

        Integer result;
        boolean done;

        CompletableFuture<Boolean> cf;

        SumIntEnumerator(AsyncEnumerator<T> source, Function<? super T, ? extends Number> selector) {
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
        public Integer current() {
            return result;
        }

        void collectSource() {
            if (getAndIncrement() == 0) {
                do {
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
                sum += selector.apply(source.current()).intValue();
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
    }
}

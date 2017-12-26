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
import java.util.function.BiConsumer;

final class AsyncInterval implements AsyncEnumerable<Long> {

    final long initialDelay;

    final long period;

    final TimeUnit unit;

    final ScheduledExecutorService executor;

    AsyncInterval(long initialDelay, long period, TimeUnit unit, ScheduledExecutorService executor) {
        this.initialDelay = initialDelay;
        this.period = period;
        this.unit = unit;
        this.executor = executor;
    }

    @Override
    public AsyncEnumerator<Long> enumerator() {
        IntervalEnumerator enumerator = new IntervalEnumerator();
        enumerator.task = executor.scheduleAtFixedRate(enumerator, initialDelay, period, unit);
        return enumerator;
    }

    static final class IntervalEnumerator
            extends AtomicInteger
            implements AsyncEnumerator<Long>, Runnable {

        final AtomicLong available;

        Future<?> task;

        long emitted;

        volatile CompletableFuture<Boolean> completable;

        Long result;

        IntervalEnumerator() {
            available = new AtomicLong();
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            result = null;
            CompletableFuture<Boolean> cf = new CompletableFuture<>();
            completable = cf;
            drain();
            return cf;
        }

        @Override
        public Long current() {
            return result;
        }

        @Override
        public void run() {
            available.getAndIncrement();
            drain();
        }

        void drain() {
            if (getAndIncrement() == 0) {
                do {
                    if (emitted != available.get()) {
                        result = emitted++;
                        completable.complete(true);
                    }
                } while (decrementAndGet() != 0);
            }
        }

        @Override
        public void cancel() {
            task.cancel(false);
        }
    }
}

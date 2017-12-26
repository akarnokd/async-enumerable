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

final class AsyncTimer implements AsyncEnumerable<Long> {

    final long time;

    final TimeUnit unit;

    final ScheduledExecutorService executor;

    AsyncTimer(long time, TimeUnit unit, ScheduledExecutorService executor) {
        this.time = time;
        this.unit = unit;
        this.executor = executor;
    }

    @Override
    public AsyncEnumerator<Long> enumerator() {
        TimerEnumerator en = new TimerEnumerator();
        en.task = executor.schedule(en, time, unit);
        return en;
    }

    static final class TimerEnumerator implements AsyncEnumerator<Long>, Callable<Void> {

        final CompletableFuture<Boolean> single = new CompletableFuture<>();

        Long result;

        boolean once;

        Future<?> task;

        @Override
        public CompletionStage<Boolean> moveNext() {
            if (once) {
                result = null;
                return FALSE;
            }
            once = true;
            return single;
        }

        @Override
        public Long current() {
            return result;
        }

        @Override
        public Void call() throws Exception {
            result = 0L;
            single.complete(true);
            return null;
        }

        @Override
        public void cancel() {
            task.cancel(false);
        }
    }
}

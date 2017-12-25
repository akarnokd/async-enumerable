package hu.akarnokd.asyncenum;

import java.util.concurrent.*;
import java.util.function.LongPredicate;

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
        executor.schedule(en, time, unit);
        return en;
    }

    static final class TimerEnumerator implements AsyncEnumerator<Long>, Callable {

        final CompletableFuture<Boolean> single = new CompletableFuture<>();

        Long result;

        boolean once;

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
        public Object call() throws Exception {
            result = 0L;
            single.complete(true);
            return null;
        }
    }
}

package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

public class AsyncTimeoutTest {

    @Test
    public void noTimeout() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        try {
            List<Integer> list = AsyncEnumerable.range(1, 5)
            .timeout(1, TimeUnit.MINUTES, scheduler)
            .toList()
            .blockingFirst()
            ;

            assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    public void withTimeout() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        try {
            List<Integer> list = AsyncEnumerable.<Integer>never()
                    .timeout(100, TimeUnit.MILLISECONDS, scheduler, AsyncEnumerable.range(1, 5))
                    .toList()
                    .blockingFirst()
                    ;

            assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
        } finally {
            scheduler.shutdownNow();
        }
    }
}

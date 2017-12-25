package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

public class AsyncTimerTest {

    @Test
    public void simple() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        try {
            List<Long> list =
                    AsyncEnumerable.timer(100, TimeUnit.MILLISECONDS, scheduler)
                    .toList()
                    .blockingFirst();

            assertEquals(Collections.singletonList(0L), list);
        } finally {
            scheduler.shutdownNow();
        }
    }
}

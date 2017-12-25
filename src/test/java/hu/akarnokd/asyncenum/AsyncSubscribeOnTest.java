package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsyncSubscribeOnTest {

    @Test
    public void simple() {
        ExecutorService exec = Executors.newSingleThreadExecutor(r -> new Thread(r, "CustomPool"));
        try {
            List<String> list = AsyncEnumerable.range(1, 5)
                    .subscribeOn(exec)
                    .map(v -> v + " " + Thread.currentThread().getName())
                    .toList()
                    .blockingFirst();

            assertEquals(5, list.size());
            for (String s : list) {
                assertTrue(s, s.contains("CustomPool"));
            }
        } finally {
            exec.shutdownNow();
        }
    }
}

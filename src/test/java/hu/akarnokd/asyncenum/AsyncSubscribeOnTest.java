package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsyncSubscribeOnTest {

    @Test
    public void simple() {
        List<String> list = AsyncEnumerable.range(1, 5)
                .subscribeOn(ForkJoinPool.commonPool())
                .map(v -> v + " " + Thread.currentThread().getName())
                .toList()
                .blockingFirst();

        assertEquals(5, list.size());
        for (String s : list) {
            assertTrue(s, s.contains("ForkJoinPool"));
        }
    }
}

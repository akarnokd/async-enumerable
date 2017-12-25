package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsyncRangeTest {

    @Test
    public void shortRange() throws Exception {
        runRange(10);
    }

    void runRange(int n) throws Exception {
        List<Integer> list = new ArrayList<>();
        Boolean result = AsyncEnumerable.range(1, n)
                .forEach(list::add)
                .toCompletableFuture()
                .get();

        assertTrue(result);
        assertEquals(n, list.size());

        for (int i = 1; i <= n; i++) {
            assertEquals(i, list.get(i - 1).intValue());
        }
    }

    @Test
    public void longRange() throws Exception {
        runRange(1_000_000);
    }
}

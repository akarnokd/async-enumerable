package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class AsyncTakeTest {

    @Test
    public void simple() throws Exception {
        List<Integer> list = new ArrayList<>();
        AsyncEnumerable.range(1, 5)
                .take(3)
                .forEach(list::add)
                .toCompletableFuture()
                .get();

        assertEquals(Arrays.asList(1, 2, 3), list);
    }
}

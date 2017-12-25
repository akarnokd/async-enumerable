package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class AsyncFlatMapTest {

    @Test
    public void synchronous() throws Exception {
        List<Integer> list = new ArrayList<Integer>();
        AsyncEnumerable.range(1, 5)
                .flatMap(v -> AsyncEnumerable.range(v, 2))
                .forEach(list::add)
                .toCompletableFuture()
                .get();

        // due to prefetch 1, the result is the breadth-first collection
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 2, 3, 4, 5, 6), list);
    }
}

package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class AsyncFromIterableTest {

    @Test
    public void simple() throws Exception {
        List<Integer> list = new ArrayList<>();
        AsyncEnumerable.fromIterable(Arrays.asList(1, 2, 3, 4, 5))
                .forEach(list::add)
                .toCompletableFuture()
                .get();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }
}

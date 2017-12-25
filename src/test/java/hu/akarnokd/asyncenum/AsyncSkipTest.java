package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class AsyncSkipTest {

    @Test
    public void simple() throws Exception {
        List<Integer> list = new ArrayList<>();
        AsyncEnumerable.range(1, 5)
                .skip(3)
                .forEach(list::add)
                .toCompletableFuture()
                .get();

        assertEquals(Arrays.asList(4, 5), list);
    }

    @Test
    public void sorter() throws Exception {
        List<Integer> list = new ArrayList<>();
        AsyncEnumerable.range(1, 2)
                .skip(3)
                .forEach(list::add)
                .toCompletableFuture()
                .get();

        assertEquals(Collections.emptyList(), list);
    }

    @Test
    public void simpleLong() throws Exception {
        List<Integer> list = new ArrayList<>();
        AsyncEnumerable.range(1, 1_000_000)
                .skip(999_997)
                .forEach(list::add)
                .toCompletableFuture()
                .get();

        assertEquals(Arrays.asList(999_998, 999_999, 1_000_000), list);
    }
}

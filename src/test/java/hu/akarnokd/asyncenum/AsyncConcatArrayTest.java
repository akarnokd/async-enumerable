package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class AsyncConcatArrayTest {

    @Test
    public void simple() throws Exception {
        List<Integer> list = new ArrayList<>();
        AsyncEnumerable.concat(
                AsyncEnumerable.range(1, 3),
                AsyncEnumerable.range(4, 2))
                .forEach(list::add)
                .toCompletableFuture()
                .get();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void simpleLong() throws Exception {
        List<Integer> list = new ArrayList<>();
        AsyncEnumerable<Integer>[] sources = new AsyncEnumerable[1_000_000];
        for (int i = 1; i < sources.length - 1; i++) {
            sources[i] = AsyncEnumerable.empty();
        }
        sources[0] = AsyncEnumerable.range(1, 3);
        sources[999_999] = AsyncEnumerable.range(4, 2);

        AsyncEnumerable.concat(sources)
                .forEach(list::add)
                .toCompletableFuture()
                .get();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }
}

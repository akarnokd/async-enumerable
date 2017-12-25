package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class AsyncConcataMapTest {

    @Test
    public void simple() {
        List<Integer> list = AsyncEnumerable.range(1, 5)
                .concatMap(v -> AsyncEnumerable.range(v, 2))
                .toList()
                .blockingFirst();

        assertEquals(Arrays.asList(1, 2, 2, 3, 3, 4, 4, 5, 5, 6), list);
    }

    @Test
    public void simpleLong() {
        List<Integer> list = AsyncEnumerable.range(0, 1_000_000)
                .concatMap(AsyncEnumerable::just)
                .toList()
                .blockingFirst();

        for (int i = 0; i < 1_000_000; i++) {
            assertEquals(i, list.get(i).intValue());
        }
    }

    @Test
    public void simpleLongEmpty() {
        List<Integer> list = AsyncEnumerable.range(0, 1_000_000)
                .concatMap(v -> AsyncEnumerable.<Integer>empty())
                .toList()
                .blockingFirst();

        assertEquals(Collections.emptyList(), list);
    }

    @Test
    public void crossMap() {
        List<Integer> list = AsyncEnumerable.range(0, 1_000)
                .concatMap(v -> AsyncEnumerable.range(v, 1000))
                .toList()
                .blockingFirst();
        for (int i = 0; i < 1_000; i++) {
            for (int j = i; j < i + 1000; j++) {
                assertEquals(j, list.get(i * 1_000 + (j - i)).intValue());
            }
        }
    }
}

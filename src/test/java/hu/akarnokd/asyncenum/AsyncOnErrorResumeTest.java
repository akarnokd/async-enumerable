package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class AsyncOnErrorResumeTest {

    @Test
    public void simple() {
        List<Integer> list = AsyncEnumerable.range(1, 5)
                .onErrorResume(v -> AsyncEnumerable.range(6, 5))
                .toList()
                .blockingFirst();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void withError() {
        List<Integer> list = AsyncEnumerable.<Integer>error(new IOException())
                .onErrorResume(v -> AsyncEnumerable.range(6, 5))
                .toList()
                .blockingFirst();

        assertEquals(Arrays.asList(6, 7, 8, 9, 10), list);
    }
}

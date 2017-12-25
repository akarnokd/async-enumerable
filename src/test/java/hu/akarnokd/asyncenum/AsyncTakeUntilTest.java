package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;

public class AsyncTakeUntilTest {

    @Test
    public void simple() {
        CompletableFuture<String> cf = new CompletableFuture<>();

        List<Integer> list = AsyncEnumerable.range(1, 5)
                .takeUntil(AsyncEnumerable.fromCompletionStage(cf))
                .toList()
                .blockingFirst();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void simpleOther() {
        CompletableFuture<String> cf = CompletableFuture.completedFuture("");

        List<Integer> list = AsyncEnumerable.range(1, 5)
                .takeUntil(AsyncEnumerable.fromCompletionStage(cf))
                .toList()
                .blockingFirst();

        assertEquals(Collections.emptyList(), list);
    }
}

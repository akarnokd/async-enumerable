package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;

public class AsyncCompletionStageTest {
    @Test
    public void simple() {

        List<Integer> list = AsyncEnumerable.fromCompletionStage(CompletableFuture.completedStage(1))
                .toList()
                .blockingFirst();

        assertEquals(Collections.singletonList(1), list);
    }
}

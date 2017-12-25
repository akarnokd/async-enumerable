/*
 * Copyright 2017 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

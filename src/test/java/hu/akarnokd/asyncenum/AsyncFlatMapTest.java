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


    @Test
    public void synchronousTake() throws Exception {
        List<Integer> list =
        AsyncEnumerable.range(1, 5)
                .flatMap(v -> AsyncEnumerable.range(v, 2))
                .take(5)
                .toList()
                .blockingFirst();

        // due to prefetch 1, the result is the breadth-first collection
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void mainError() {
        TestHelper.assertFailure(
                AsyncEnumerable.error(new RuntimeException("forced failure"))
                        .flatMap(v -> AsyncEnumerable.just(1)),
                RuntimeException.class, "forced failure"
        );
    }

    @Test
    public void innerError() {
        TestHelper.assertFailure(
                AsyncEnumerable.range(1, 5)
                        .flatMap(v -> AsyncEnumerable.error(new RuntimeException("forced failure"))),
                RuntimeException.class, "forced failure"
        );
    }
}

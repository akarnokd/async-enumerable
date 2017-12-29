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

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AsyncGroupByTest {

    @Test
    public void monoGroup() {
        List<Integer> list = new ArrayList<Integer>();
        AsyncEnumerable.range(1, 5)
                .groupBy(v -> 1)
                .forEach(g -> g.forEach(list::add));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void cancelGroup() {
        List<Integer> list = new ArrayList<Integer>();
        AsyncEnumerable.range(1, 5)
                .groupBy(v -> 1)
                .forEach(g -> g.take(1).forEach(list::add));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }


    @Test
    public void cancelMain() {
        List<Integer> list = new ArrayList<Integer>();
        AsyncEnumerable.range(1, 5)
                .groupBy(v -> 1)
                .take(1)
                .forEach(g -> g.forEach(list::add));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }


    @Test
    public void cancelMainDifferentGroup() {
        List<Integer> list = new ArrayList<Integer>();
        AsyncEnumerable.range(1, 5)
                .groupBy(v -> v & 1)
                .take(1)
                .forEach(g -> g.forEach(list::add));

        assertEquals(Arrays.asList(1, 3, 5), list);
    }


    @Test
    public void cancelMainDifferentGroupTakeInner() {
        List<Integer> list = new ArrayList<Integer>();
        AsyncEnumerable.range(1, 5)
                .groupBy(v -> v & 1)
                .take(1)
                .forEach(g -> g.take(2).forEach(list::add));

        assertEquals(Arrays.asList(1, 3), list);
    }

    @Test
    public void cancelMainExact() {
        List<Integer> list = new ArrayList<Integer>();
        AsyncEnumerable.range(1, 5)
                .groupBy(v -> 1)
                .take(5)
                .forEach(g -> g.forEach(list::add));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }


    @Test
    public void cancelMainExact2() {
        List<Integer> list = new ArrayList<Integer>();
        AsyncEnumerable.range(1, 5)
                .groupBy(v -> 1)
                .take(5)
                .forEach(g -> g.take(5).forEach(list::add));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void cancelGroupAndMain() {
        List<Integer> list = new ArrayList<Integer>();
        AsyncEnumerable.range(1, 5)
                .groupBy(v -> 1)
                .take(1)
                .forEach(g -> g.take(1).forEach(list::add));

        assertEquals(Collections.singletonList(1), list);
    }

    @Test
    public void someGroup() {
        List<Integer> list = new ArrayList<Integer>();
        AsyncEnumerable.range(1, 5)
                .groupBy(v -> v & 1)
                .forEach(g -> g.forEach(list::add));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void allGroups() {
        List<Integer> groups = new ArrayList<Integer>();
        List<Integer> list = new ArrayList<Integer>();
        AsyncEnumerable.range(1, 5)
                .groupBy(v -> v)
                .forEach(g -> {
                    groups.add(g.key());
                    g.forEach(list::add);
                });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), groups);
    }

    @Test
    public void error() throws InterruptedException {
        AtomicReference<CompletionStage<Boolean>> ref = new AtomicReference<>();
        CompletionStage<Boolean> cf = AsyncEnumerable.just(1).concatWith(AsyncEnumerable.error(new IOException()))
                .groupBy(v -> 1, u -> u)
        .forEach(g -> ref.set(g.forEach(v -> { })));

        try {
            cf.toCompletableFuture().get();
            fail("Should have thrown");
        } catch (ExecutionException ex) {
            if (!(ex.getCause() instanceof IOException)) {
                throw new AssertionError(ex);
            }
        }

        try {
            ref.get().toCompletableFuture().get();
            fail("Should have thrown");
        } catch (ExecutionException ex) {
            if (!(ex.getCause() instanceof IOException)) {
                throw new AssertionError(ex);
            }
        }
    }

    @Test
    public void oneEnumerablePerGroup() throws InterruptedException {
        AtomicReference<CompletionStage<Boolean>> ref = new AtomicReference<>();
        AsyncEnumerable.just(1)
                .groupBy(v -> 1)
                .forEach(g -> {
                    g.forEach(v -> { });
                    ref.set(g.enumerator().moveNext());
                });

        try {
            ref.get().toCompletableFuture().get();
            fail("Should have thrown");
        } catch (ExecutionException ex) {
            if (!(ex.getCause() instanceof IllegalStateException)) {
                throw new AssertionError(ex);
            }
        }
    }
}

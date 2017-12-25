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

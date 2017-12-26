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

public class AsyncZipArrayTest {

    @Test
    public void simple() {
        List<Integer> list = AsyncEnumerable.zipArray(
                a -> (Integer)a[0] + (Integer)a[1],
                AsyncEnumerable.range(1, 5),
                AsyncEnumerable.range(10, 5)
        )
        .toList()
        .blockingFirst();

        assertEquals(Arrays.asList(10 + 1, 11 + 2, 12 + 3, 13 + 4, 14 + 5), list);
    }


    @Test
    public void oneShorter() {
        List<Integer> list = AsyncEnumerable.zipArray(
                a -> (Integer)a[0] + (Integer)a[1],
                AsyncEnumerable.range(1, 4),
                AsyncEnumerable.range(10, 5)
        )
                .toList()
                .blockingFirst();

        assertEquals(Arrays.asList(10 + 1, 11 + 2, 12 + 3, 13 + 4), list);
    }

    @Test
    public void twoShorter() {
        List<Integer> list = AsyncEnumerable.zipArray(
                a -> (Integer)a[0] + (Integer)a[1],
                AsyncEnumerable.range(1, 5),
                AsyncEnumerable.range(10, 4)
        )
                .toList()
                .blockingFirst();

        assertEquals(Arrays.asList(10 + 1, 11 + 2, 12 + 3, 13 + 4), list);
    }

    @Test
    public void simpleWith() {
        List<Integer> list = AsyncEnumerable.range(1, 5)
                .zipWith(AsyncEnumerable.range(10, 5), (a, b) -> a + b)
                .toList()
                .blockingFirst();

        assertEquals(Arrays.asList(10 + 1, 11 + 2, 12 + 3, 13 + 4, 14 + 5), list);
    }
}

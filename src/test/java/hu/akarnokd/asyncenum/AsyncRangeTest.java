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
import static org.junit.Assert.assertTrue;

public class AsyncRangeTest {

    @Test
    public void shortRange() throws Exception {
        runRange(10);
    }

    void runRange(int n) throws Exception {
        List<Integer> list = new ArrayList<>();
        Boolean result = AsyncEnumerable.range(1, n)
                .forEach(list::add)
                .toCompletableFuture()
                .get();

        assertTrue(result);
        assertEquals(n, list.size());

        for (int i = 1; i <= n; i++) {
            assertEquals(i, list.get(i - 1).intValue());
        }
    }

    @Test
    public void longRange() throws Exception {
        runRange(1_000_000);
    }
}

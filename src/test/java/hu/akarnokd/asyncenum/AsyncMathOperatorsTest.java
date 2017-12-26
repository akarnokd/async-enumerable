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

public class AsyncMathOperatorsTest {

    @Test
    public void sumInt() {
        List<Integer> list =
                AsyncEnumerable.range(1, 5)
                .sumInt(v -> v)
                .toList()
                .blockingFirst();

        assertEquals(Collections.singletonList(15), list);
    }

    @Test
    public void sumLong() {
        List<Long> list =
                AsyncEnumerable.range(1, 5)
                        .sumLong(v -> v)
                        .toList()
                        .blockingFirst();

        assertEquals(Collections.singletonList(15L), list);
    }


    @Test
    public void max() {
        List<Integer> list =
                AsyncEnumerable.range(1, 5)
                        .max(Comparator.<Integer>naturalOrder().reversed())
                        .toList()
                        .blockingFirst();

        assertEquals(Collections.singletonList(1), list);
    }

    @Test
    public void max2() {
        TestHelper.assertResult(
                AsyncEnumerable.fromArray(5, 1, 3, 2, 4)
                        .max(Comparator.<Integer>naturalOrder().reversed())
                ,
                1
        );
    }

    @Test
    public void maxEmpty() {
        TestHelper.assertResult(AsyncEnumerable.<Integer>empty().max(Comparator.naturalOrder()));
    }

    @Test
    public void sumIntEmpty() {
        TestHelper.assertResult(AsyncEnumerable.<Integer>empty().sumInt(v -> v));
    }

    @Test
    public void sumLongEmpty() {
        TestHelper.assertResult(AsyncEnumerable.<Integer>empty().sumLong(v -> v));
    }

    @Test
    public void maxError() {
        TestHelper.assertFailure(
                AsyncEnumerable.<Integer>error(new RuntimeException("forced failure"))
                .max(Comparator.naturalOrder()),
                RuntimeException.class, "forced failure"
        );
    }

    @Test
    public void sumIntError() {
        TestHelper.assertFailure(
                AsyncEnumerable.<Integer>error(new RuntimeException("forced failure"))
                        .sumInt(v -> v),
                RuntimeException.class, "forced failure"
        );
    }

    @Test
    public void sumLongError() {
        TestHelper.assertFailure(
                AsyncEnumerable.<Integer>error(new RuntimeException("forced failure"))
                        .sumLong(v -> v),
                RuntimeException.class, "forced failure"
        );
    }
}

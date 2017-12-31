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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class AsyncUsingTest {

    @Test
    public void since() {
        AtomicReference<Integer> res = new AtomicReference<>();
        TestHelper.assertResult(
                AsyncEnumerable.using(() -> 1, v -> AsyncEnumerable.range(v, 5), res::set),
                1, 2, 3, 4, 5
        );

        assertEquals(1, res.get().intValue());
    }


    @Test
    public void take() {
        AtomicReference<Integer> res = new AtomicReference<>();
        TestHelper.assertResult(
                AsyncEnumerable.using(() -> 1, v -> AsyncEnumerable.range(v, 5), res::set).take(3),
                1, 2, 3
        );

        assertEquals(1, res.get().intValue());
    }

    @Test
    public void error() {
        AtomicReference<Integer> res = new AtomicReference<>();

        TestHelper.assertFailure(
                AsyncEnumerable.using(() -> 1, v -> AsyncEnumerable.<Integer>error(new IOException("" + v)), res::set),
                IOException.class, "1"
        );

        assertEquals(1, res.get().intValue());
    }
}

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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsyncFromStreamTest {

    @Test
    public void simple() {
        TestHelper.assertResult(
                AsyncEnumerable.fromStream(IntStream.range(1, 1 + 5).boxed()),
                1, 2, 3, 4, 5
        );
    }

    @Test
    public void error() {
        Stream<Integer> stream = IntStream.range(1, 1 + 5).boxed();
        assertEquals(5, stream.count());
        TestHelper.assertFailure(
            AsyncEnumerable.fromStream(stream),
            IllegalStateException.class
        );
    }


    @Test
    public void close() {
        AtomicBoolean bool = new AtomicBoolean();
        TestHelper.assertResult(
                AsyncEnumerable.fromStream(IntStream.range(1, 1 + 5).boxed()
                        .onClose(() -> bool.set(true))),
                1, 2, 3, 4, 5
        );

        assertTrue(bool.get());
    }

}

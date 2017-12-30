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

public class AsyncTakeLastTest {

    @Test
    public void simple() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                .takeLast(2),
                4, 5
        );
    }

    @Test
    public void simpleEmpty() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                        .takeLast(0)
        );
    }

    @Test
    public void simpleEmptySource() {
        TestHelper.assertResult(
                AsyncEnumerable.empty()
                        .takeLast(2)
        );
    }

    @Test
    public void simpleAll() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                        .takeLast(5),
                1, 2, 3, 4, 5
        );
    }


    @Test
    public void simpleAll2() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                        .takeLast(6),
                1, 2, 3, 4, 5
        );
    }

    @Test
    public void error() {
        TestHelper.assertFailure(
                AsyncEnumerable.error(new IOException())
                .takeLast(2),
                IOException.class
        );
    }

    @Test
    public void cancelRace() {
        TestHelper.cancelRace(ae -> ae.takeLast(2));
    }
}

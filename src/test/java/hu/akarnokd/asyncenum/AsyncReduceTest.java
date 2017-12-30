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

public class AsyncReduceTest {

    @Test
    public void simple() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                .reduce((a, b) -> a + b),
                15
        );
    }


    @Test
    public void simpleEmpty() {
        TestHelper.assertResult(
                AsyncEnumerable.<Integer>empty()
                        .reduce((a, b) -> a + b)
        );
    }

    @Test
    public void error() {
        TestHelper.assertFailure(
                AsyncEnumerable.<Integer>error(new IOException())
                .reduce((a, b) -> a + b),
                IOException.class
        );
    }

    @Test
    public void cancelRace() {
        TestHelper.cancelRace(ae -> ae.reduce((a, b) -> a + b));
    }
}

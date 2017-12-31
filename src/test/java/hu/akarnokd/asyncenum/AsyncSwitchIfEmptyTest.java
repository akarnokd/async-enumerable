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

public class AsyncSwitchIfEmptyTest {

    @Test
    public void nonEmpty() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                .switchIfEmpty(AsyncEnumerable.range(6, 5)),
                1, 2, 3, 4, 5
        );
    }

    @Test
    public void empty() {
        TestHelper.assertResult(
                AsyncEnumerable.<Integer>empty()
                        .switchIfEmpty(AsyncEnumerable.range(6, 5)),
                6, 7, 8, 9, 10
        );
    }


    @Test
    public void nonEmptyTake() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                        .switchIfEmpty(AsyncEnumerable.range(6, 5))
                        .take(3),
                1, 2, 3
        );
    }

    @Test
    public void emptyTake() {
        TestHelper.assertResult(
                AsyncEnumerable.<Integer>empty()
                        .switchIfEmpty(AsyncEnumerable.range(6, 5))
                        .take(3),
                6, 7, 8
        );
    }


    @Test
    public void emptyEmpty() {
        TestHelper.assertResult(
                AsyncEnumerable.<Integer>empty()
                        .switchIfEmpty(AsyncEnumerable.empty())
        );
    }

    @Test
    public void error() {
        TestHelper.assertFailure(
                AsyncEnumerable.<Integer>error(new IOException())
                .switchIfEmpty(AsyncEnumerable.range(1, 5)),
                IOException.class
        );
    }


    @Test
    public void emptyError() {
        TestHelper.assertFailure(
                AsyncEnumerable.empty()
                        .switchIfEmpty(AsyncEnumerable.error(new IOException())),
                IOException.class
        );
    }
}

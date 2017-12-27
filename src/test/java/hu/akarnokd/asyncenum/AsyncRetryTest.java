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
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncRetryTest {

    @Test
    public void simple() {
        TestHelper.assertResult(
                AsyncEnumerable.just(1)
                .retry(5),
                1
        );
    }

    @Test
    public void take() {
        TestHelper.assertResult(
                AsyncEnumerable.repeatItem(1)
                        .retry(5)
                        .take(5),
                1, 1, 1, 1, 1
        );
    }

    @Test
    public void error() {
        TestHelper.assertFailure(
                AsyncEnumerable.error(new IOException())
                .retry(1),
                IOException.class
        );
    }

    @Test
    public void stop() {
        TestHelper.assertResult(
                AsyncEnumerable.just(1)
                        .retry(e -> false),
                1
        );
    }

    @Test
    public void rightError() {
        AtomicInteger count = new AtomicInteger();

        TestHelper.assertResult(
            AsyncEnumerable.defer(() -> {
                if (count.getAndIncrement() == 0) {
                    return AsyncEnumerable.<Integer>error(new IOException());
                }
                return AsyncEnumerable.range(1, 5);
            })
            .retry(1),
            1, 2, 3, 4, 5
        );
    }

    @Test
    public void wrongError() {
        AtomicInteger count = new AtomicInteger();

        TestHelper.assertFailure(
                AsyncEnumerable.error(new IOException())
                .retry(e -> e instanceof RuntimeException),
                IOException.class
        );
    }
}

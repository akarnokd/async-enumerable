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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncRetryWhenTest {

    @Test
    public void simple() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                .retryWhen(e -> CompletableFuture.completedStage(false))
                ,
                1, 2, 3, 4, 5
        );
    }

    @Test
    public void take() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                        .retryWhen(e -> CompletableFuture.completedStage(false))
                        .take(3),
                1, 2, 3
        );
    }

    @Test
    public void error() {
        TestHelper.assertResult(
                AsyncEnumerable.error(new IOException())
                .retryWhen(AtomicInteger::new, (s, e) -> CompletableFuture.completedStage(s.incrementAndGet() < 5))
        );
    }

    @Test
    public void completerFails() {
        TestHelper.assertFailure(
                AsyncEnumerable.error(new IOException())
                .retryWhen(AtomicInteger::new, (s, e) -> CompletableFuture.failedStage(e)),
                IOException.class
        );

    }
}

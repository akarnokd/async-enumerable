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
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsyncObserveOnTest {

    @Test
    public void simple() {
        ExecutorService exec = Executors.newSingleThreadExecutor(r -> new Thread(r, "CustomPool"));
        try {
            List<String> list = AsyncEnumerable.range(1, 5)
                    .observeOn(exec)
                    .map(v -> v + " " + Thread.currentThread().getName())
                    .toList()
                    .blockingFirst();

            assertEquals(5, list.size());
            for (String s : list) {
                assertTrue(s, s.contains("CustomPool"));
            }
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    public void error() {
        TestHelper.withExecutor(exec -> {
            TestHelper.assertFailure(
                    AsyncEnumerable.error(new IllegalArgumentException("forced failure"))
                            .observeOn(exec)
                    , IllegalArgumentException.class, "forced failure");
        });
    }
}

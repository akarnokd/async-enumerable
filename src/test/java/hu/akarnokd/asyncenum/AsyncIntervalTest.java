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
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

public class AsyncIntervalTest {

    @Test
    public void simple() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            List<Long> list = AsyncEnumerable.interval(1, TimeUnit.MILLISECONDS, executor)
                    .take(5)
                    .toList()
                    .blockingFirst();

            assertEquals(Arrays.asList(0L, 1L, 2L, 3L, 4L), list);
        } finally {
            executor.shutdownNow();
        }
    }
}

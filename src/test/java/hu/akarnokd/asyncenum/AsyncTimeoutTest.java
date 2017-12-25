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

public class AsyncTimeoutTest {

    @Test
    public void noTimeout() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        try {
            List<Integer> list = AsyncEnumerable.range(1, 5)
            .timeout(1, TimeUnit.MINUTES, scheduler)
            .toList()
            .blockingFirst()
            ;

            assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    public void withTimeout() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        try {
            List<Integer> list = AsyncEnumerable.<Integer>never()
                    .timeout(100, TimeUnit.MILLISECONDS, scheduler, AsyncEnumerable.range(1, 5))
                    .toList()
                    .blockingFirst()
                    ;

            assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
        } finally {
            scheduler.shutdownNow();
        }
    }
}

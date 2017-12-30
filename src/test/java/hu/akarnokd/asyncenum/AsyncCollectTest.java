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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class AsyncCollectTest {

    @Test
    public void error() {
        try {
            AsyncEnumerable.error(new RuntimeException("forced failure"))
                    .collect(HashMap::new, (a, b) -> a.put(b, b))
                    .blockingFirst();
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getMessage().equals("forced failure"));
        }
    }

    @Test
    public void streamCollector() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                .collect(Collectors.toList()),
                Arrays.asList(1, 2, 3, 4, 5)
        );
    }

    @Test
    public void streamCollectorError() {
        TestHelper.assertFailure(
                AsyncEnumerable.error(new IOException())
                .collect(Collectors.toList()),
                IOException.class
        );
    }

    @Test
    public void cancel() {
        AtomicBoolean bool = new AtomicBoolean();

        AsyncEnumerable.never()
                .doOnCancel(() -> bool.set(true))
                .collect(Collectors.toList())
                .enumerator()
                .cancel();

        assertTrue(bool.get());
    }
}

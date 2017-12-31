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
import java.util.stream.*;

import static org.junit.Assert.*;

public class AsyncBlockingIterableTest {

    @Test
    public void simple() {
        List<Integer> list = new ArrayList<>();
        for (Integer i : AsyncEnumerable.range(1, 5).blockingIterable()) {
            list.add(i);
        }

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }


    @Test
    public void simpleAsync() {
        TestHelper.withExecutor(executor -> {
            List<Integer> list = new ArrayList<>();
            for (Integer i : AsyncEnumerable.range(1, 5)
                    .observeOn(executor).blockingIterable()) {
                list.add(i);
            }

            assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
        });
    }

    @Test
    public void stream() {
        List<Integer> list =
                AsyncEnumerable.range(1, 5)
                .blockingStream()
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void streamAsync() {
        TestHelper.withExecutor(executor -> {
            List<Integer> list =
                    AsyncEnumerable.range(1, 5)
                            .observeOn(executor)
                            .blockingStream()
                            .collect(Collectors.toList());

            assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
        });
    }

    @Test
    public void streamCancel() {
        AtomicBoolean bool = new AtomicBoolean();

        try (Stream<Integer> str = AsyncEnumerable.range(1, 5).doOnCancel(() -> bool.set(true)).blockingStream()) {
            assertEquals(1, str.iterator().next().intValue());
        }

        assertTrue(bool.get());
    }

    @Test
    public void error() {
        try {
            AsyncEnumerable.error(new RuntimeException("forced failure"))
                    .blockingIterable()
                    .iterator()
                    .next();
            fail("Should have thrown");
        } catch (RuntimeException ex) {
            if (!ex.getMessage().equals("forced failure")) {
                throw new AssertionError(ex);
            }
        }
    }

    @Test
    public void errorChecked() {
        try {
            AsyncEnumerable.error(new IOException("forced failure"))
                    .blockingIterable()
                    .iterator()
                    .next();
            fail("Should have thrown");
        } catch (RuntimeException ex) {
            if (!ex.getCause().getMessage().equals("forced failure")) {
                throw new AssertionError(ex);
            }
        }
    }


    @Test
    public void errorInterrupted() {
        try {
            Thread.currentThread().interrupt();
            AsyncEnumerable.never()
                    .blockingIterable()
                    .iterator()
                    .next();
            fail("Should have thrown");
        } catch (RuntimeException ex) {
            if (!(ex.getCause() instanceof InterruptedException)) {
                throw new AssertionError(ex);
            }
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void noSuchElement() {
        AsyncEnumerable.empty()
                .blockingIterable()
                .iterator()
                .next();
    }
}

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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class AsyncEnumeratorHelperTest {

    @Test
    public void doubleCancel() {
        AtomicReference<AsyncEnumerator<Integer>> ref = new AtomicReference<>();

        assertTrue(AsyncEnumeratorHelper.cancel(ref));
        assertFalse(AsyncEnumeratorHelper.cancel(ref));
    }

    @Test
    public void replaceRace() {
        TestHelper.withExecutor(executor -> {

            AtomicReference<AsyncEnumerator<Object>> ref = new AtomicReference<>();

            for (int i = 0; i < 10000; i++) {
                Runnable r1 = () -> AsyncEnumeratorHelper.replace(ref, AsyncEmpty.INSTANCE);

                Runnable r2 = () -> AsyncEnumeratorHelper.cancel(ref);

                TestHelper.race(r1, r2, executor);

                assertTrue(AsyncEnumeratorHelper.isCancelled(ref.get()));
            }
        });
    }


    @Test
    public void replaceRace2() {
        TestHelper.withExecutor(executor -> {

            AtomicReference<AsyncEnumerator<Object>> ref = new AtomicReference<>();

            for (int i = 0; i < 10000; i++) {
                Runnable r1 = () -> AsyncEnumeratorHelper.replace(ref, AsyncEmpty.INSTANCE);

                Runnable r2 = () -> AsyncEnumeratorHelper.replace(ref, AsyncNever.INSTANCE);

                TestHelper.race(r1, r2, executor);
            }
        });
    }

    @Test
    public void cancelledCurrentNull() {
        assertNull(AsyncEnumeratorHelper.CANCELLED.current());
    }

    @Test
    public void cancelledMoveNext() throws InterruptedException {
        assertSame(AsyncEnumerable.CANCELLED, AsyncEnumeratorHelper.CANCELLED.moveNext());
    }
}

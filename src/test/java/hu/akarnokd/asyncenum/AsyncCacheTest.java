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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncCacheTest {

    @Test
    public void simple() {
        AsyncEnumerable<Integer> ae = AsyncEnumerable.range(1, 5).cache();

        TestHelper.assertResult(ae, 1, 2, 3, 4, 5);
        TestHelper.assertResult(ae, 1, 2, 3, 4, 5);
    }


    @Test
    public void async() {
        TestHelper.withScheduler(executor -> {
            AtomicInteger cnt = new AtomicInteger();

            AsyncEnumerable<Integer> ae =
                AsyncEnumerable.timer(100, TimeUnit.MILLISECONDS, executor)
                .flatMap(v -> AsyncEnumerable.range(cnt.incrementAndGet(), 5))
                .cache();

            TestHelper.assertResult(ae, 1, 2, 3, 4, 5);
            TestHelper.assertResult(ae, 1, 2, 3, 4, 5);

            TestHelper.assertResult(ae.take(3), 1, 2, 3);
        });
    }

    @Test
    public void error() {
        AtomicInteger cnt = new AtomicInteger();

        AsyncEnumerable<Object> ae =
                AsyncEnumerable.defer(() ->
                        AsyncEnumerable.error(new IOException("failure " + cnt.incrementAndGet())))
                        .cache();

        TestHelper.assertFailure(ae, IOException.class, "failure 1");
        TestHelper.assertFailure(ae, IOException.class, "failure 1");
    }

    @Test
    public void doubleCancel() {
        AsyncEnumerable<Integer> ae = AsyncEnumerable.range(1, 5).cache();

        AsyncEnumerator<Integer> en = ae.enumerator();
        en.cancel();
        en.cancel();
    }

    @Test
    public void enumeratorRace() {
        TestHelper.withExecutor(executor -> {
            for (int i = 0; i < 10000; i++) {
                AsyncEnumerable<Integer> ae = AsyncEnumerable.<Integer>never().cache();

                TestHelper.race(ae::enumerator, ae::enumerator, executor);
            }
        });
    }


    @Test
    public void remove() {
        AsyncEnumerable<Integer> ae = AsyncEnumerable.<Integer>never().cache();

        AsyncEnumerator<Integer> en1 = ae.enumerator();
        AsyncEnumerator<Integer> en2 = ae.enumerator();
        AsyncEnumerator<Integer> en3 = ae.enumerator();

        en2.cancel();
        en1.cancel();
        en1.cancel();
        en3.cancel();
    }


    @Test
    public void addRemoveRace() {
        TestHelper.withExecutor(executor -> {
            for (int i = 0; i < 10000; i++) {
                AsyncEnumerable<Integer> ae = AsyncEnumerable.<Integer>never().cache();

                AsyncEnumerator<Integer> en1 = ae.enumerator();

                TestHelper.race(ae::enumerator, en1::cancel, executor);
            }
        });
    }
}

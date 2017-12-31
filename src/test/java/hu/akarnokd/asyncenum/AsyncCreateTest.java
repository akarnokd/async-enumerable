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
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.Assert.assertTrue;

public class AsyncCreateTest {

    @Test
    public void simple() {
        TestHelper.assertResult(
                AsyncEnumerable.create(emitter -> {
                    for (int i = 1; i <= 5; i++) {
                        emitter.next(i);
                    }
                    emitter.stop();
                }),
                1, 2, 3, 4, 5
        );
    }

    @Test
    public void simpleWithResource() {
        AtomicBoolean bool = new AtomicBoolean();
        TestHelper.assertResult(
                AsyncEnumerable.create(emitter -> {
                    emitter.setResource(() -> bool.set(true));
                    for (int i = 1; i <= 5; i++) {
                        emitter.next(i);
                    }
                    emitter.stop();
                }),
                1, 2, 3, 4, 5
        );

        assertTrue(bool.get());
    }

    @Test
    public void simpleWithResourceSwap() {
        AtomicBoolean bool = new AtomicBoolean();
        AtomicBoolean bool2 = new AtomicBoolean();
        AtomicBoolean bool3 = new AtomicBoolean();
        AtomicBoolean bool4 = new AtomicBoolean();
        TestHelper.assertResult(
                AsyncEnumerable.create(emitter -> {
                    emitter.setResource(() -> bool.set(true));
                    emitter.setResource(() -> bool2.set(true));
                    for (int i = 1; i <= 5; i++) {
                        if (emitter.isCancelled()) {
                            return;
                        }
                        emitter.next(i);
                    }
                    emitter.stop();
                    bool3.set(emitter.isCancelled());

                    emitter.next(6);
                    emitter.stop();
                    emitter.error(new IOException());
                    emitter.setResource(() -> bool4.set(true));
                    emitter.setResource(AsyncCreate.Closed.INSTANCE);
                }),
                1, 2, 3, 4, 5
        );

        assertTrue(bool.get());
        assertTrue(bool2.get());
        assertTrue(bool3.get());
        assertTrue(bool4.get());
    }

    @Test
    public void error() {
        AtomicBoolean bool = new AtomicBoolean();

        TestHelper.assertFailure(
                AsyncEnumerable.create(emitter -> {
                    emitter.setResource(() -> bool.set(true));
                    emitter.error(new IOException());
                }),
                IOException.class
        );

        assertTrue(bool.get());
    }


    @Test
    public void resourceSwapRace() {
        TestHelper.assertResult(
                AsyncEnumerable.create(emitter -> {
                    TestHelper.withExecutor(executor -> {
                        for (int i = 0; i < 10000; i++) {
                            TestHelper.race(
                                    () -> emitter.setResource(() -> {
                                    }),
                                    () -> emitter.setResource(() -> {
                                    }),
                                    executor
                            );
                        }
                    });
                    emitter.stop();
                })
        );
    }

    @Test
    public void async() {
        TestHelper.withScheduler(executor -> {
            TestHelper.assertResult(
                    AsyncEnumerable.create(emitter -> {
                        Future<?> f = executor.schedule(() -> {
                            for (int i = 1; i <= 10; i++) {
                                while (emitter.emissionPending() != 0 || emitter.isCancelled()) {}
                                if (emitter.isCancelled()) {
                                    return;
                                }
                                emitter.next(i);
                            }
                            emitter.stop();
                        }, 100, TimeUnit.MILLISECONDS);
                        emitter.setResource(() -> f.cancel(false));
                    }).take(5)
                    , 1, 2, 3, 4, 5
            );
        });
    }

    @Test
    public void resourceCrash() {
        Thread.UncaughtExceptionHandler eh = Thread.currentThread().getUncaughtExceptionHandler();
        try {
            AtomicReference<Throwable> ex = new AtomicReference<>();
            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> ex.set(e));

            TestHelper.assertResult(
                    AsyncEnumerable.create(emitter -> {
                        emitter.setResource(() -> { throw new IOException("forced failure"); });
                        emitter.stop();
                    })
            );

            assertTrue(ex.toString(), ex.get() instanceof IOException && ex.get().getMessage().equals("forced failure"));
        } finally {
            Thread.currentThread().setUncaughtExceptionHandler(eh);
        }
    }
}

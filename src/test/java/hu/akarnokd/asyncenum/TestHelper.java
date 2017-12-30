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

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import static org.junit.Assert.*;

public final class TestHelper {

    private TestHelper() {
        throw new IllegalStateException("No instances");
    }

    public static void assertFailure(AsyncEnumerable<?> source, Class<? extends Throwable> exception) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch cdl = new CountDownLatch(1);
        source.forEach(v -> { }).whenComplete((b, t) -> {
            error.set(t);
            cdl.countDown();
        });
        try {
            assertTrue(cdl.await(5, TimeUnit.SECONDS));
            Throwable ex = error.get();
            if (ex != null) {
                if (!exception.isInstance(ex)) {
                    ex.printStackTrace();
                    throw new AssertionError("Wrong exception", ex);
                }
                return;
            }
            fail("Should have failed");
        } catch (InterruptedException ex) {
            throw new AssertionError(ex);
        }
    }

    public static void assertFailure(AsyncEnumerable<?> source, Class<? extends Throwable> exception, String message) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch cdl = new CountDownLatch(1);
        source.forEach(v -> { }).whenComplete((b, t) -> {
            error.set(t);
            cdl.countDown();
        });
        try {
            assertTrue(cdl.await(5, TimeUnit.SECONDS));
            Throwable ex = error.get();
            if (ex != null) {
                if (!exception.isInstance(ex)) {
                    ex.printStackTrace();
                    throw new AssertionError("Wrong exception", ex);
                }
                assertEquals("Wrong message", message, ex.getMessage());
                return;
            }
            fail("Should have failed");
        } catch (InterruptedException ex) {
            throw new AssertionError(ex);
        }
    }

    public static void withExecutor(Consumer<? super ExecutorService> consumer) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            consumer.accept(exec);
        } finally {
            exec.shutdownNow();
        }
    }

    public static void withScheduler(Consumer<? super ScheduledExecutorService> consumer) {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            consumer.accept(exec);
        } finally {
            exec.shutdownNow();
        }
    }

    @SafeVarargs
    public static <T> void assertResult(AsyncEnumerable<T> source, T... items) {
        try {
            List<T> result = source.toList().blockingLast();
            assertEquals(Arrays.asList(items), result);
        } catch (AssertionError ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new AssertionError("unexpected failure", ex);
        }
    }

    public static void race(Runnable r1, Runnable r2, Executor executor) {
        AtomicReference<Throwable> error2 = new AtomicReference<>();
        CountDownLatch cdl = new CountDownLatch(1);
        AtomicInteger sync = new AtomicInteger(2);

        executor.execute(() -> {
            if (sync.decrementAndGet() != 0) {
                while (sync.get() != 0) { }
            }
            try {
                r2.run();
            } catch (Throwable ex) {
                error2.set(ex);
            }
            cdl.countDown();
        });

        if (sync.decrementAndGet() != 0) {
            while (sync.get() != 0) { }
        }

        Throwable error1 = null;
        try {
            r1.run();
        } catch (Throwable ex) {
            error1 = ex;
        }

        try {
            if (!cdl.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("The second Runnable timed out");
            }
        } catch (InterruptedException ex) {
            throw new AssertionError("Wait interrupted", ex);
        }

        if (error1 != null && error2.getPlain() != null) {
            AssertionError ex = new AssertionError("Both Runnables failed");
            ex.addSuppressed(error1);
            ex.addSuppressed(error2.getPlain());
            return;
        }
        if (error1 != null && error2.getPlain() == null) {
            throw new AssertionError("First Runnable failed", error1);
        }
        if (error1 == null && error2.getPlain() != null) {
            throw new AssertionError("Second Runnable failed", error2.getPlain());
        }
    }

    public static void checkUtility(Class<?> clazz) {
        try {
            Constructor c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            c.newInstance();

        } catch (Throwable ex) {
            if ((ex.getCause() instanceof IllegalStateException)
                    && ex.getCause().getMessage().equals("No instances!")) {
                return;
            }
            throw new AssertionError("Wrong exception type or message", ex);
        }
        throw new AssertionError("Not an utility class!");
    }

    public static <U> void cancelRace(Function<? super AsyncEnumerable<Integer>, ? extends AsyncEnumerable<U>> transformer) {
        TestHelper.withExecutor(executor -> {
            for (int i = 0; i < 10000; i++) {
                AsyncEnumerator<U> en = AsyncEnumerable.range(1, 1_000_000_000)
                        .compose(transformer)
                        .enumerator();

                TestHelper.race(en::moveNext,
                        en::cancel, executor);
            }
        });
    }
}

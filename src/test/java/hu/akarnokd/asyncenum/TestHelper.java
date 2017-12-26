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

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class TestHelper {

    private TestHelper() {
        throw new IllegalStateException("No instances");
    }

    public static void assertFailure(AsyncEnumerable<?> source, Class<? extends Throwable> exception) {
        try {
            source.blockingLast();
            fail("Should have thrown");
        } catch (Throwable ex) {
            if (!exception.isInstance(ex)) {
                ex.printStackTrace();
                throw new AssertionError("Wrong exception", ex);
            }
        }
    }

    public static void assertFailure(AsyncEnumerable<?> source, Class<? extends Throwable> exception, String message) {
        try {
            source.blockingLast();
            fail("Should have thrown");
        } catch (Throwable ex) {
            if (!exception.isInstance(ex)) {
                ex.printStackTrace();
                throw new AssertionError("Wrong exception", ex);
            }
            assertEquals("Wrong message", message, ex.getMessage());
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
}

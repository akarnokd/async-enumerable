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
import java.util.NoSuchElementException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AsyncBlockingFirstTest {

    @Test(expected = NoSuchElementException.class)
    public void empty() {
        AsyncEnumerable.empty().blockingFirst();
    }

    @Test
    public void interrupt() {
        Thread.currentThread().interrupt();

        try {
            AsyncEnumerable.never().blockingFirst();
            fail("Should have thrown");
        } catch (RuntimeException ex) {
            assertTrue("" + ex, ex.getCause() instanceof InterruptedException);
        }
    }

    @Test
    public void checkedException() {
        try {
            AsyncEnumerable.error(new IOException()).blockingFirst();
            fail("Should have thrown");
        } catch (RuntimeException ex) {
            assertTrue("" + ex, ex.getCause() instanceof IOException);
        }
    }
}

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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

public class AsyncGenerateTest {

    @Test
    public void empty() {
        TestHelper.assertResult(
                AsyncEnumerable.generate(SyncEmitter::stop)
        );
    }


    @Test
    public void errorNoSignal() {
        TestHelper.assertFailure(
                AsyncEnumerable.generate(e -> { }),
                IllegalStateException.class
        );
    }

    @Test
    public void error() {
        TestHelper.assertFailure(
                AsyncEnumerable.generate(e -> e.error(new IOException())),
                IOException.class
        );
    }

    @Test
    public void one() {
        TestHelper.assertResult(
                AsyncEnumerable.generate(e -> {
                    e.next(1);
                    e.stop();
                }),
                1
        );
    }


    @Test
    public void oneTwoRounds() {
        TestHelper.assertResult(
                AsyncEnumerable.generate(() -> 0, (s, e) -> {
                    if (s == 0) {
                        e.next(1);
                    } else {
                        e.stop();
                    }
                    return s + 1;
                }),
                1
        );
    }


    @Test
    public void oneError() {
        TestHelper.assertFailure(
                AsyncEnumerable.generate(e -> {
                    e.next(1);
                    e.error(new IOException());
                }),
                IOException.class
        );
    }


    @Test
    public void oneErrorTwoRounds() {
        TestHelper.assertFailure(
                AsyncEnumerable.generate(() -> 0, (s, e) -> {
                    if (s == 0) {
                        e.next(1);
                    } else {
                        e.error(new IOException());
                    }
                    return s + 1;
                }, s -> { }),
                IOException.class
        );
    }

    @Test
    public void stopCleanup() {
        AtomicBoolean bool = new AtomicBoolean();

        TestHelper.assertResult(
                AsyncEnumerable.generate(() -> 1, (s, e) -> {
                    if (s == 6) {
                        e.stop();
                    } else {
                        e.next(s);
                    }
                    return s + 1;
                }, s -> bool.set(true)),
                1, 2, 3, 4, 5
        );

        assertTrue(bool.get());
    }

    @Test
    public void errorCleanup() {
        AtomicBoolean bool = new AtomicBoolean();

        TestHelper.assertFailure(
                AsyncEnumerable.<Integer, Integer>generate(() -> 1, (s, e) -> {
                    if (s == 6) {
                        e.error(new IOException());
                    } else {
                        e.next(s);
                    }
                    return s + 1;
                }, s -> bool.set(true)),
                IOException.class
        );

        assertTrue(bool.get());
    }


    @Test
    public void cancelCleanup() {
        AtomicBoolean bool = new AtomicBoolean();

        TestHelper.assertResult(
                AsyncEnumerable.generate(() -> 1, (s, e) -> {
                    if (s == 6) {
                        e.stop();
                    } else {
                        e.next(s);
                    }
                    return s + 1;
                }, s -> bool.set(true))
                .take(3),
                1, 2, 3
        );

        assertTrue(bool.get());
    }

    @Test
    public void multiNext() {
        TestHelper.assertFailure(
                AsyncEnumerable.generate(e -> {
                    e.next(1);
                    e.next(2);
                    e.next(3);
                }),
                IllegalStateException.class, "next() called multiple times"
        );
    }


    @Test
    public void multiError() {
        TestHelper.assertFailure(
                AsyncEnumerable.generate(e -> {
                    e.error(new IOException());
                    e.error(new IOException());
                    e.error(new IOException());
                }),
                IllegalStateException.class, "error() called multiple times"
        );
    }
}

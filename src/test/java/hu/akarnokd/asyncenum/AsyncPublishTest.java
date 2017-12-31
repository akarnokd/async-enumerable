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

public class AsyncPublishTest {

    @Test
    public void passthrough() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                .publish(v -> v),
                1, 2, 3, 4, 5
        );
    }

    @Test
    public void passthroughError() {
        TestHelper.assertFailure(
                AsyncEnumerable.error(new IOException())
                .publish(v -> v),
                IOException.class
        );
    }

    @Test
    public void doubleSum() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                .publish(v -> v.sumInt(u -> u).mergeWith(v.sumInt(u -> u))),
                15, 15
        );
    }

    @Test
    public void simpleTransform() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                .publish(v -> v.map(u -> u * 2)),
                2, 4, 6, 8, 10
        );
    }


    @Test
    public void simpleTransformTake() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                        .publish(v -> v.map(u -> u * 2))
                .take(3),
                2, 4, 6
        );
    }

    @Test
    public void innerTake() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                        .publish(an -> an.take(3)),
                1, 2, 3
        );
    }

    @Test
    public void spiltCombine() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                .publish(an -> an.take(3).mergeWith(an.takeLast(2))),
                1, 2, 3, 4, 5
        );
    }


    @Test
    public void spiltCombineEvenOdd() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                        .publish(an -> an.filter(v -> v % 2 == 0).mergeWith(an.filter(v -> v % 2 != 0))),
                1, 2, 3, 4, 5
        );
    }

    @Test
    public void spiltCombineConcat() {
        TestHelper.assertResult(
                AsyncEnumerable.range(1, 5)
                        .publish(an ->
                                an.take(3)
                                .concatWith(an.takeLast(2))),
                1, 2, 3, 4, 5
        );
    }

    @Test
    public void lateInner() {
        TestHelper.assertResult(
            AsyncEnumerable.range(1, 5)
                    .publish(an -> an.concatWith(an))
                , 1, 2, 3, 4, 5
        );
    }

    @Test
    public void cancelRace() {
        TestHelper.cancelRace(an -> an.publish(f -> f));
    }

    @Test
    public void cancelRace2() {
        TestHelper.cancelRace(an -> an.publish(f -> f.map(v -> v)));
    }

    @Test
    public void addRace() {
        TestHelper.assertResult(
                AsyncEnumerable.empty()
                .publish(an -> {
                    Runnable r = an.enumerator()::cancel;
                    TestHelper.withExecutor(executor -> {
                        for (int i = 0; i < 10000; i++) {
                            TestHelper.race(r, r, executor);

                        }
                    });
                    return an;
                })
        );
    }

    @Test
    public void addRace2() {
        TestHelper.assertResult(
                AsyncEnumerable.empty()
                        .publish(an -> {
                            Runnable r = () -> {
                                AsyncEnumerator<Object> en = an.enumerator();
                                en.cancel();
                                en.cancel();
                            };
                            TestHelper.withExecutor(executor -> {
                                for (int i = 0; i < 10000; i++) {
                                    TestHelper.race(r, r, executor);
                                }
                            });
                            return an;
                        })
        );
    }
}

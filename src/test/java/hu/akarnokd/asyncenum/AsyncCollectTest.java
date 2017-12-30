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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import java.util.stream.*;

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

    @Test
    public void cancelStopsSource() {
        TestHelper.withScheduler(executor -> {
            TestHelper.assertResult(AsyncEnumerable.range(1, 1_000_000_000)
                    .collect(() -> 0, (a, b) -> { })
                    .timeout(100, TimeUnit.MILLISECONDS, executor, AsyncEnumerable.empty())
            );
        });
    }


    @Test
    public void cancelStopsSource2() {
        TestHelper.withScheduler(executor -> {
            TestHelper.assertResult(AsyncEnumerable.range(1, 1_000_000_000)
                    .collect(new Collector<Integer, Object, Object>() {
                        @Override
                        public Supplier<Object> supplier() {
                            return () -> 0;
                        }

                        @Override
                        public BiConsumer<Object, Integer> accumulator() {
                            return (a, b) -> { };
                        }

                        @Override
                        public BinaryOperator<Object> combiner() {
                            return (a, b) -> a;
                        }

                        @Override
                        public Function<Object, Object> finisher() {
                            return a -> a;
                        }

                        @Override
                        public Set<Characteristics> characteristics() {
                            return Set.of(Characteristics.UNORDERED);
                        }
                    })
                    .timeout(100, TimeUnit.MILLISECONDS, executor, AsyncEnumerable.empty())
            );
        });
    }
}

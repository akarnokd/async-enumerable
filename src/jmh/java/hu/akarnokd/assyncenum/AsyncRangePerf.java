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

package hu.akarnokd.assyncenum;

import hu.akarnokd.asyncenum.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class AsyncRangePerf extends AtomicInteger implements BiConsumer<Boolean, Throwable> {

    @Param({"1", "10", "100", "1000", "10000", "100000", "1000000"})
    int count;

    AsyncEnumerable<Integer> range;

    AsyncEnumerator<Integer> en;

    Blackhole bh;

    @Setup
    public void setup(Blackhole bh) {
        range = AsyncEnumerable.range(1, count);
        this.bh = bh;
    }

    @Benchmark
    public void range() {
        en = range.enumerator();
        consume();
    }

    void consume() {
        if (getAndIncrement() == 0) {
            do {
                en.moveNext().whenComplete(this);
            } while (decrementAndGet() != 0);
        }
    }

    @Override
    public void accept(Boolean aBoolean, Throwable throwable) {
        if (throwable != null) {
            bh.consume(throwable);
            return;
        }

        if (aBoolean) {
            bh.consume(en.current());
            consume();
        } else {
            bh.consume(false);
        }
    }
}

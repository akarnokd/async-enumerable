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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.Assert.*;

public class AsyncFromFlowPublisherTest {

    @Test
    public void simple() {
        SubmissionPublisher<Integer> sp = new SubmissionPublisher<>();

        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(100);
                } catch (InterruptedException ex) {
                    sp.closeExceptionally(ex);
                    return;
                }
                for (int i = 0; i < 1000; i++) {
                    sp.submit(i);
                }
                sp.close();
            }
        }.start();

        List<Integer> list = AsyncEnumerable.fromFlowPublisher(sp)
                .toList()
                .blockingFirst();

        assertEquals(1000, list.size());

        for (int i = 0; i < 1000; i++) {
            assertEquals(i, list.get(i).intValue());
        }
    }

    @Test
    public void error() throws InterruptedException {
        SubmissionPublisher<Integer> sp = new SubmissionPublisher<>();
        CompletionStage<Boolean> cf = AsyncEnumerable.fromFlowPublisher(sp).enumerator().moveNext();

        sp.closeExceptionally(new RuntimeException("forced failure"));

        try {
            cf.toCompletableFuture().get();
            fail("Should have thrown");
        } catch (ExecutionException ex) {
            assertTrue(ex.toString(), ex.getCause().getMessage().equals("forced failure"));
        }
    }

    @Test
    public void doubleOnSubscribe() {
        BooleanSubscription s1 = new BooleanSubscription();
        BooleanSubscription s2 = new BooleanSubscription();

        assertEquals((Integer)1, AsyncEnumerable.fromFlowPublisher(subscriber -> {
            subscriber.onSubscribe(s1);
            subscriber.onSubscribe(s2);
            subscriber.onNext(1);
            subscriber.onComplete();
        })
        .blockingLast());

        assertFalse(s1.get());
        assertTrue(s2.get());
    }

    @Test
    public void take() {
        TestHelper.withExecutor(executor -> {
        SubmissionPublisher<Integer> sp = new SubmissionPublisher<>();

            executor.submit(() -> {
                Thread.sleep(100);
                for (int i = 0; i < 5; i++) {
                    sp.submit(i);
                }
                sp.close();
                return null;
            });

            List<Integer> list = AsyncEnumerable.fromFlowPublisher(sp)
                    .take(3)
                    .toList()
                    .blockingLast();

            assertEquals(Arrays.asList(0, 1, 2), list);

            assertEquals(0, sp.getNumberOfSubscribers());
        });
    }

    @Test
    public void cancelledSubscription() {
        AsyncFromFlowPublisher.CancelledSubscription.CANCELLED.request(1L);
        AsyncFromFlowPublisher.CancelledSubscription.CANCELLED.cancel();
    }

    @Test
    public void delayedOnSubscribe() {
        TestHelper.withExecutor((ExecutorService executor) -> {
            for (int i = 0; i < 10000; i++) {
                AtomicReference<Flow.Subscriber<? super Integer>> sub = new AtomicReference<>();
                AsyncEnumerator<Integer> cf = AsyncEnumerable.<Integer>fromFlowPublisher(sub::set)
                        .enumerator();
                AtomicReference<CompletionStage<Boolean>> nx = new AtomicReference<>();

                TestHelper.race(
                        () -> {
                            nx.set(cf.moveNext());
                        },
                        () -> {
                            sub.get().onSubscribe(new BooleanSubscription());
                        },
                        executor
                );


                sub.get().onComplete();

                try {
                    assertFalse(nx.get().toCompletableFuture().get());
                } catch (Throwable ex) {
                    throw new AssertionError(ex);
                }
            }
        });
    }

    static final class BooleanSubscription extends AtomicBoolean implements Flow.Subscription {

        @Override
        public void request(long n) {
            // deliberately ignored
        }

        @Override
        public void cancel() {
            set(true);
        }
    }
}

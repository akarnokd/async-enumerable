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
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsyncToFlowPublisherTest {

    @Test
    public void simple() {
        List<Object> list = new ArrayList<>();
        AsyncEnumerable.range(1, 5)
                .toFlowPublisher()
                .subscribe(new Flow.Subscriber<>() {

                    Flow.Subscription upstream;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        upstream = subscription;
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(Integer item) {
                        list.add(item);
                        upstream.request(1);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        list.add(throwable);
                    }

                    @Override
                    public void onComplete() {
                        list.add("done");
                    }
                });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, "done"), list);
    }


    @Test
    public void simpleError() {
        List<Object> list = new ArrayList<>();
        AsyncEnumerable.range(1, 5)
                .concatWith(AsyncEnumerable.error(new RuntimeException("error")))
                .toFlowPublisher()
                .subscribe(new Flow.Subscriber<>() {

                    Flow.Subscription upstream;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        upstream = subscription;
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(Integer item) {
                        list.add(item);
                        upstream.request(1);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        list.add(throwable);
                    }

                    @Override
                    public void onComplete() {
                        list.add("done");
                    }
                });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list.subList(0, 5));

        assertTrue("" + list.get(5), ((Throwable)list.get(5)).getMessage().equals("error"));
    }


    @Test
    public void simpleEmptyNoRequest() {
        List<Object> list = new ArrayList<>();
        AsyncEnumerable.<Integer>empty()
                .toFlowPublisher()
                .subscribe(new Flow.Subscriber<>() {

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                    }

                    @Override
                    public void onNext(Integer item) {
                        list.add(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        list.add(throwable);
                    }

                    @Override
                    public void onComplete() {
                        list.add("done");
                    }
                });

        assertEquals(Collections.singletonList("done"), list);
    }

    @Test
    public void simpleErrorNoRequest() {
        List<Object> list = new ArrayList<>();
        AsyncEnumerable.<Integer>error(new RuntimeException("forced failure"))
                .toFlowPublisher()
                .subscribe(new Flow.Subscriber<>() {

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                    }

                    @Override
                    public void onNext(Integer item) {
                        list.add(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        list.add(throwable);
                    }

                    @Override
                    public void onComplete() {
                        list.add("done");
                    }
                });

        assertEquals(1, list.size());

        assertTrue(list.get(0).toString(), ((Throwable)list.get(0)).getMessage().equals("forced failure"));
    }


    @Test
    public void simpleManyRequest() {
        List<Object> list = new ArrayList<>();
        AsyncEnumerable.<Integer>never()
                .toFlowPublisher()
                .subscribe(new Flow.Subscriber<>() {

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscription.request(100);
                        subscription.request(Long.MAX_VALUE);
                        subscription.request(0);
                    }

                    @Override
                    public void onNext(Integer item) {
                        list.add(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        list.add(throwable);
                    }

                    @Override
                    public void onComplete() {
                        list.add("done");
                    }
                });

        assertEquals(1, list.size());

        assertTrue(list.get(0).toString(), ((IllegalArgumentException)list.get(0)).getMessage().contains("3.9"));
    }


    @Test
    public void requestCancel() {
        List<Object> list = new ArrayList<>();
        AsyncEnumerable.range(1, 5)
                .toFlowPublisher()
                .subscribe(new Flow.Subscriber<>() {

                    Flow.Subscription upstream;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        upstream = subscription;
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(Integer item) {
                        list.add(item);
                        upstream.cancel();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        list.add(throwable);
                    }

                    @Override
                    public void onComplete() {
                        list.add("done");
                    }
                });

        assertEquals(Collections.singletonList(1), list);
    }


    @Test
    public void requestCancel2() {
        List<Object> list = new ArrayList<>();
        AsyncEnumerable.range(1, 5)
                .toFlowPublisher()
                .subscribe(new Flow.Subscriber<>() {

                    Flow.Subscription upstream;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        upstream = subscription;
                        subscription.request(100);
                    }

                    @Override
                    public void onNext(Integer item) {
                        list.add(item);
                        upstream.cancel();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        list.add(throwable);
                    }

                    @Override
                    public void onComplete() {
                        list.add("done");
                    }
                });

        assertEquals(Collections.singletonList(1), list);
    }


    @Test
    public void cancel() {
        List<Object> list = new ArrayList<>();
        AsyncEnumerable.<Integer>error(new RuntimeException("forced failure"))
                .toFlowPublisher()
                .subscribe(new Flow.Subscriber<>() {

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscription.cancel();
                    }

                    @Override
                    public void onNext(Integer item) {
                        list.add(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        list.add(throwable);
                    }

                    @Override
                    public void onComplete() {
                        list.add("done");
                    }
                });

        assertEquals(0, list.size());
    }

    @Test
    public void requestRace() {
        TestHelper.withExecutor(executor -> {
            for (int i = 0; i < 10000; i++) {
                AtomicReference<Flow.Subscription> sub = new AtomicReference<>();

                List<Object> list = new ArrayList<>();
                AsyncEnumerable.<Integer>never()
                        .toFlowPublisher()
                        .subscribe(new Flow.Subscriber<>() {

                            @Override
                            public void onSubscribe(Flow.Subscription subscription) {
                                sub.set(subscription);
                            }

                            @Override
                            public void onNext(Integer item) {
                                list.add(item);
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                list.add(throwable);
                            }

                            @Override
                            public void onComplete() {
                                list.add("done");
                            }
                        });

                Runnable r = () -> sub.get().request(1);

                TestHelper.race(r, r, executor);

                assertEquals(0, list.size());
            }
        });
    }
}

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
}

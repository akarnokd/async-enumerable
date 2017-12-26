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

import static org.junit.Assert.*;

public class AsyncDoOnXTest {

    @Test
    public void onNext() {
        List<Integer> list1 = new ArrayList<>();

        List<Integer> list =
                AsyncEnumerable.range(1, 5)
                .doOnNext(list1::add)
                .toList()
                .blockingFirst();

        assertEquals(list1, list);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }


    @Test
    public void onError() {
        List<Throwable> list1 = new ArrayList<>();

        try {
            AsyncEnumerable.error(new RuntimeException("forced failure"))
            .doOnError(list1::add)
            .blockingLast()
            ;
            fail("Should have thrown");
        } catch (RuntimeException ex) {
            assertEquals("forced failure", ex.getMessage());
        }

        assertTrue(list1.toString(), list1.get(0).getMessage().equals("forced failure"));
    }

    @Test
    public void onComplete() {
        List<Integer> list1 = new ArrayList<>();

        List<Integer> list =
                AsyncEnumerable.range(1, 5)
                        .doOnComplete(() -> list1.add(100))
                        .toList()
                        .blockingFirst();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
        assertEquals(Collections.singletonList(100), list1);
    }

    @Test
    public void doFinally() {

        List<Integer> list1 = new ArrayList<>();

        List<Integer> list =
                AsyncEnumerable.range(1, 5)
                        .doFinally(() -> list1.add(100))
                        .toList()
                        .blockingFirst();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
        assertEquals(Collections.singletonList(100), list1);
    }

    @Test
    public void doFinallyCancel() {

        List<Integer> list1 = new ArrayList<>();

        AsyncEnumerator<Integer> cf = AsyncEnumerable.range(1, 5)
                        .doFinally(() -> list1.add(100))
                        .enumerator();

        cf.cancel();

        assertEquals(Collections.singletonList(100), list1);
    }


    @Test
    public void doFinallyError() {

        List<Integer> list1 = new ArrayList<>();

        TestHelper.assertFailure(
            AsyncEnumerable.error(new RuntimeException("forced failure"))
                .doFinally(() -> list1.add(100)),
                RuntimeException.class, "forced failure"
        );

        assertEquals(Collections.singletonList(100), list1);
    }

}

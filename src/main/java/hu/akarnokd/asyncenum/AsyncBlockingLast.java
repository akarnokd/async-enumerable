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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

final class AsyncBlockingLast<T> extends CountDownLatch implements BiConsumer<Boolean, Throwable> {

    final AsyncEnumerator<T> source;

    final AtomicInteger wip;

    boolean hasValue;
    T result;

    Throwable error;

    AsyncBlockingLast(AsyncEnumerator<T> source) {
        super(1);
        this.source = source;
        this.wip = new AtomicInteger();
    }

    void moveNext() {
        if (wip.getAndIncrement() == 0) {
            do {
                source.moveNext().whenComplete(this);
            } while (wip.decrementAndGet() != 0);
        }
    }

    @Override
    public void accept(Boolean aBoolean, Throwable throwable) {
        if (throwable != null) {
            result = null;
            error = throwable;
            countDown();
            return;
        }

        if (aBoolean) {
            hasValue = true;
            result = source.current();
            moveNext();
        } else {
            countDown();
        }
    }

    T blockingGet() {
        if (getCount() != 0) {
            try {
                await();
            } catch (InterruptedException ex) {
                source.cancel();
                throw new RuntimeException(ex);
            }
        }
        Throwable ex = error;
        if (ex != null) {
            throw ThrowableHelper.wrapOrThrow(ex);
        }
        if (hasValue) {
            return result;
        }
        throw new NoSuchElementException();
    }


    Optional<T> blockingGetOptional() {
        if (getCount() != 0) {
            try {
                await();
            } catch (InterruptedException ex) {
                source.cancel();
                throw new RuntimeException(ex);
            }
        }
        Throwable ex = error;
        if (ex != null) {
            throw ThrowableHelper.wrapOrThrow(ex);
        }
        if (hasValue) {
            return Optional.ofNullable(result);
        }
        return Optional.empty();
    }
}

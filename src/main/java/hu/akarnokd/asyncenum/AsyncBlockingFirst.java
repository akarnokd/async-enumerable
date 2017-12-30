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
import java.util.concurrent.ExecutionException;

final class AsyncBlockingFirst {

    private AsyncBlockingFirst() {
        throw new IllegalStateException("No instances!");
    }

    public static <T> T blockingFirst(AsyncEnumerator<T> source) {
        try {
            Boolean result = source.moveNext().toCompletableFuture().get();
            if (result) {
                T r = source.current();
                source.cancel();
                return r;
            }
            throw new NoSuchElementException();
        } catch (InterruptedException ex) {
            source.cancel();
            throw new RuntimeException(ex);
        } catch (ExecutionException ex) {
            throw ThrowableHelper.wrapOrThrow(ex.getCause());
        }
    }

    public static <T> Optional<T> blockingFirstOptional(AsyncEnumerator<T> source) {
        try {
            Boolean result = source.moveNext().toCompletableFuture().get();
            if (result) {
                T r = source.current();
                source.cancel();
                return Optional.ofNullable(r);
            }
            return Optional.empty();
        } catch (InterruptedException ex) {
            source.cancel();
            throw new RuntimeException(ex);
        } catch (ExecutionException ex) {
            throw ThrowableHelper.wrapOrThrow(ex.getCause());
        }
    }
}

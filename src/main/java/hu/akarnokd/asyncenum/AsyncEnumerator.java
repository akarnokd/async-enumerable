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

import java.util.concurrent.CompletionStage;

public interface AsyncEnumerator<T> {

    /**
     * Asks the AsyncEnumerator to fetch the next item and notify its availability
     * by completing the returned CompletionStage with: {@code true} if there is
     * an item is ready to be read via {@link #current()}; {@code false} if
     * there won't be any more items; or containing the {@code Throwable} indicating
     * an error.
     * <p>
     *     The method should not be called if it was called recently and the
     *     CompletionStage hasn't terminated yet.
     * </p>
     * @return the CompletionStage that gets terminated depending on there are more
     * items or an error available.
     */
    CompletionStage<Boolean> moveNext();

    /**
     * Returns the current item when the CompletionStage returned by {@link #moveNext()}
     * completes with {@code true}.
     * <p>
     *     This method should not be called without calling
     *     {@code moveNext()} first and while the CompletionStage of {@code moveNext()} hasn't
     *     completed yet or has been completed with {@code false} or with a {@code Throwable}.
     * </p>
     * @return the item, may be null
     */
    T current();

    /**
     * Instructs the AsyncEnumerator to cancel any outstanding async activity and
     * release resources associated with it.
     */
    // FIXME make mandatory
    default void cancel() {

    }
}

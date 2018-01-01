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
import java.util.function.Consumer;

final class AsyncRange implements AsyncEnumerable<Integer> {

    final int start;

    final int count;

    AsyncRange(int start, int count) {
        this.start = start;
        this.count = count;
    }

    @Override
    public AsyncEnumerator<Integer> enumerator() {
        return new AsyncRangeEnumerator(start, start + count);
    }

    static final class AsyncRangeEnumerator implements AsyncEnumerator<Integer> {

        final int end;

        int index;

        Integer current;

        AsyncRangeEnumerator(int start, int end) {
            this.index = start;
            this.end = end;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            int idx = index;
            if (idx == end) {
                current = null;
                return FALSE;
            }
            current = idx;
            index = idx + 1;
            return TRUE;
        }

        @Override
        public Integer current() {
            return current;
        }

        @Override
        public void cancel() {
            // No action, consumer should stop calling moveNext().
        }
    }
}

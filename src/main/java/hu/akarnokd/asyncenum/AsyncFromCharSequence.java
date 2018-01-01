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

final class AsyncFromCharSequence implements AsyncEnumerable<Integer> {

    final CharSequence array;

    AsyncFromCharSequence(CharSequence array) {
        this.array = array;
    }

    @Override
    public AsyncEnumerator<Integer> enumerator() {
        return new FromCharSequenceEnumerator(array);
    }

    static final class FromCharSequenceEnumerator implements AsyncEnumerator<Integer> {

        final CharSequence array;

        int index;

        Integer current;

        FromCharSequenceEnumerator(CharSequence array) {
            this.array = array;
        }

        @Override
        public CompletionStage<Boolean> moveNext() {
            int idx = index;
            if (idx == array.length()) {
                current = null;
                return FALSE;
            }
            current = (int)array.charAt(idx);
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

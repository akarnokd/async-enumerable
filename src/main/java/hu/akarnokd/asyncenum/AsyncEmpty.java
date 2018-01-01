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

enum AsyncEmpty implements AsyncEnumerable<Object>, AsyncEnumerator<Object> {
    INSTANCE;

    @SuppressWarnings("unchecked")
    static <T> AsyncEnumerable<T> instance() {
        return (AsyncEnumerable<T>)INSTANCE;
    }

    @Override
    public AsyncEnumerator<Object> enumerator() {
        return this;
    }

    @Override
    public CompletionStage<Boolean> moveNext() {
        return FALSE;
    }

    @Override
    public Object current() {
        return null;
    }

    @Override
    public void cancel() {
        // No action, consumer should stop calling moveNext().
    }
}

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
    }
}

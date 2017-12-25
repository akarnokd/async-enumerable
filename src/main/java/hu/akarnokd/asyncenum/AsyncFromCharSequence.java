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
    }
}

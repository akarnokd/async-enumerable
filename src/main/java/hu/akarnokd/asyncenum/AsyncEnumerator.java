package hu.akarnokd.asyncenum;

import java.util.concurrent.CompletionStage;

public interface AsyncEnumerator<T> {

    CompletionStage<Boolean> moveNext();

    T current();
}

# async-enumerable

<a href='https://travis-ci.org/akarnokd/async-enumerable/builds'><img src='https://travis-ci.org/akarnokd/async-enumerable.svg?branch=master'></a>
[![codecov.io](http://codecov.io/github/akarnokd/async-enumerable/coverage.svg?branch=master)](http://codecov.io/github/akarnokd/async-enumerable?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/async-enumerable/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/async-enumerable)

Prototype library based on the asyncronous enumerable concept (where moveNext() returns a task to compose over). 

### Gradle

```groovy
compile "com.github.akarnokd:async-enumerable:0.1.0"
```

### Getting started

The main entry point is the `hu.akarnokd.asyncenum.AsyncEnumerable` interface with its static factory methods similar to RxJava:

```java
AsyncEnumerable<Integer> source = AsyncEnumerable.range(1, 10);

AsyncEnumerable<String> strings = AsyncEnumerable.fromArray("a", "b", "c", "d");
```

`AsyncEnumerable<T>` is a deferred cold source, which can be synchronous or asynchronous, and one 
requires to call `enumerator()` to receive another interface,
`hu.akarnokd.asyncenum.AsyncEnumerator` to be "iterated" over.

```java
AsyncEnumerator<Integer> enumerator = source.enumerator();
```

The `AsyncEnumerator<T>` defines two methods, `moveNext()` and `current()`. Calling `moveNext()` will instruct the
source to produce the next value but instead of returning a `false` or `true` immediately, the method returns a
`java.util.concurrent.CompletionStage<Boolean>` that is completed with `true` if a value is ready and `false` if no
more values to be expected. In the `true` case, one can read the current value via `current()`. 

(Cancelling a sequence is currently not supported due to still experimenting with ways to express it.)

```java
CompletionStage<Boolean> stage = enumerator.moveNext();

stage.whenComplete((hasValue, error) -> {
    if (error != null) {
        error.printStackTrace();
        return;
    }
    
    if (hasValue) {
        System.out.println(enumerator.current());
    } else {
        System.out.println("Empty source!");
    }
})
```

Note that calling `moveNext()` or `current()` during the time the `CompletionStage` hasn't been terminated is an 
undefined behavior. Calling `moveNext` after the previous `CompletionStage` returned `false` or an exception is
also undefined behavior.

Therefore, consuming multiple values via a plain for loop doesn't work; one has to call `moveNext` when the previous
`CompletionStage` completed with `true` in a recursively looking pattern. Since some `AsyncEnumerable` chains can
be synchronous, this leads to `StackOverflowError` if not handled properly. 

For this purpose, the `forEach()` instance method on `AsyncEnumerable` is available, but given an `AsyncEnumerator`, 
the following consumption pattern can be employed:

```java
final class EnumeratorConsumer<T> extends AtomicInteger implements BiConsumer<Boolean, Throwable> {
    
    final AsyncEnumerator<T> enumerator;
    
    public EnumeratorConsumer(AsyncEnumerator<T> enumerator) {
        this.enumerator = enumerator;
    }
    
    @Override
    public void accept(Boolean hasNext, Throwable error) {
        if (error != null) {
            // handle error case
            return;
        }
        if (hasNext) {
            T value = enumerator.current();
            // handle current value
            moveNext();   
        } else {
            // handle no more values
        }
    }
    
    public void moveNext() {
        if (getAndIncrement() == 0) {
            do {
                enumerator.moveNext().whenComplete(this);
            } while (decrementAndGet() != 0);
        }
    }
}

new EnumeratorConsumer(source.enumerator()).moveNext();
```

This is practically the same queue-drain or trampolining logic used throughout RxJava. It is recommended though
to use the combinators and operators of `AsyncEnumerable` instead as working with a sequence of `CompletionStage`
continuations, especially when there are multiple active sequences involved as complications.
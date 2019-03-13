package tuesday;

import java.util.stream.Stream;

public class MoooarObservables {
  public static void main(String[] args) {
    Observable<Integer> arrayObservable =
        Observable.from(10, 20, 30);

    arrayObservable
        .map(x -> x / 10)
        .filter(x -> x != 2)
        .delay(2000)
        .subscribe(
            o -> System.out.println(o),
            throwable -> System.out.println(throwable),
            () -> System.out.println("done")
        );
  }
}

class Observable<T> {
  Subscribe<T> innerSubscribe;

  public Observable(Subscribe<T> subscribe) {
    this.innerSubscribe = subscribe;
  }

  void subscribe(Observer<T> observer) {
    innerSubscribe.subscribe(observer);
  }

  void subscribe(NextCallback<T> next, ErrorCallback error, CompleteCallback complete) {
    subscribe(new Observer<>(next, error, complete));
  }

  public <R> Observable<R> map(Transform<T, R> transform) {
    Observable<T> inputObservable = this;
    Observable<R> outputObservable = Observable.create(
        outputObserver ->
            inputObservable.subscribe(
                new Observer<>(
                    t -> outputObserver.onNext(transform.transform(t)),
                    throwable -> outputObserver.onError(throwable),
                    () -> outputObserver.onComplete()
                )
            ));
    return outputObservable;
  }

  public Observable<T> filter(Condition<T> condition) {
    Observable<T> inputObservable = this;
    Observable<T> outputObservable = Observable.create(
        outputObserver ->
            inputObservable.subscribe(
                new Observer<>(
                    t -> {
                      if (condition.pass(t)) {
                        outputObserver.onNext(t);
                      }
                    },
                    throwable -> outputObserver.onError(throwable),
                    () -> outputObserver.onComplete()
                )
            ));
    return outputObservable;
  }

  public Observable<T> delay(long time) {
    Observable<T> inputObservable = this;
    Observable<T> outputObservable = Observable.create(
        outputObserver ->
            inputObservable.subscribe(
                new Observer<>(
                    t -> {
                      try {
                        Thread.sleep(time);
                      } catch (InterruptedException ignored) { }
                      outputObserver.onNext(t);
                    },
                    throwable -> outputObserver.onError(throwable),
                    () -> outputObserver.onComplete()
                )
            ));
    return outputObservable;
  }

  static <T> Observable<T> from(T... items) {
    return Observable.create(observer -> {
      Stream.of(items)
          .forEach(i -> observer.onNext(i));
      observer.onComplete();
    });
  }

  static <T> Observable<T> create(Subscribe<T> subscribe) {
    return new Observable<>(subscribe);
  }
}

class Observer<T> {
  NextCallback<T> next;
  ErrorCallback error;
  CompleteCallback complete;

  public Observer(NextCallback<T> next, ErrorCallback error, CompleteCallback complete) {
    this.next = next;
    this.error = error;
    this.complete = complete;
  }

  void onNext(T t) {
    next.nextCallback(t);
  }

  void onError(Throwable throwable) {
    error.errorCallback(throwable);
  }

  void onComplete() {
    complete.completeCallback();
  }
}

interface Transform<INPUT, OUTPUT> {
  OUTPUT transform(INPUT input);
}

interface Condition<ITEM> {
  boolean pass(ITEM item);
}

interface Subscribe<T> {
  void subscribe(Observer<T> observer);
}

interface NextCallback<T> {
  void nextCallback(T t);
}

interface ErrorCallback {
  void errorCallback(Throwable throwable);
}

interface CompleteCallback {
  void completeCallback();
}

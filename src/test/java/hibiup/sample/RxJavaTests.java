package hibiup.sample;

import io.reactivex.*;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import junit.framework.TestCase;
import org.junit.Test;
import sun.rmi.runtime.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RxJavaTests extends TestCase {

    @Test
    public void testHelloRxJava2() {
        /*
         只包括事件和事件观察者的简单的例子
         */

        /** 1）生成被观测对象: 范型参数为返回类型。可选的方法有：
         *
         *  just(T...)：直接将传入的参数来填装发送器:
         *    Observable observable = Observable.just("A", "B", "C");
         *
         *  from(T[]) 或 from(Iterable<? extends T>) : 将 iterable 对象内容依次发送出来：
         *    Observable observable = Observable.from({"A", "B", "C"})
         *
         *  just 或 from 方法根据接收的参数透明生成 OnSubscribe 对象传给 Observable。下一个例子演示如何显式地生成 OnSubscribe
         **/
        Observable<String> observable = Observable.just("Hello", "World");   // 返回 "Hello"

        /** 2）为被观测对象注册订阅者（函数对象） */
        observable.subscribe(s -> System.out.println(s));  // 订阅结果
    }

    @Test
    public void testCreateObservableAndRegisterSubscriber() {
        /*
         * 在上面的例子中，只有一个简单的被观察对象（Observable）和订阅者(Subscriber)。下面这个例子演示
         * 生成一个两个完整的 Observable 和 Subscriber。
         */

        /**
         * 1）用 create 显式传入一个 OnSubscribe 来生成 Observable 对象。
         *
         * OnSubscribe 是实际上的事件生成器，当一个 Observer 被注册上来的时候，它的 subscribe 方法就会自动触发向 Observer
         * 发送事件。OnSubscribe 通过来自 Observable 的 Emitter 对象来发送事件。
         * */
        Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                /**
                 * 2) 在 subscribe() 中。被观察对象将通过一个发射器（emitter）来发射事件。
                 **/
                System.out.println("Observable: emit 1"); emitter.onNext(1);
                System.out.println("Observable: emit 2"); emitter.onNext(2);
                System.out.println("Observable: emit 3"); emitter.onNext(3);

                // 填装完成调用 onComplete 方法
                System.out.println("Observable: call onComplete()");emitter.onComplete();
                //　如果在 onComplete 之后再发射数据将不会被 Subscriber 接收。
                System.out.println("Observable: emit 4");emitter.onNext(4);
            }
        });

        /** 3) 接下来为 Observable 注册观察者 Observer。
         *
         * Observable.subscribe 注册观察者的行为会导致上一步注册的 OnSubscribe 的 subscribe 方法被触发，从而产生出事件（流）。
         * 而这个 Observer 就是事件的处理者。*/
        observable.subscribe(new Observer<Integer>() {
            private Disposable mDisposable;

            @Override
            public void onSubscribe(@NonNull Disposable d) {
                System.out.println("Observer: call onSubscribe()" );
                mDisposable = d;
            }

            @Override
            public void onNext(@NonNull Integer integer) {
                System.out.println("Observer: call onNext() receive " + integer);
                if (integer == 2) {
                    // 在RxJava 2.x 中，新增的Disposable可以做到切断的操作，让Observer观察者不再接收上游事件
                    System.out.println("Observer: 2 will triggers isDisposable()=" + mDisposable.isDisposed());
                    mDisposable.dispose();
                    System.out.println("Observer: isDisposable()=" + mDisposable.isDisposed() + " will refuses to receive message anymore.");
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                System.out.println("Observer: call onError : " + e.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("Observer: call onComplete()" );
            }
        });
    }

    @Test
    public void testRegisterConsumer() {
        /* 如上所示，实现一个 Observer 接口需要实现多个方法．因此也可以注册一个相对简单的 Consumer 来接收事件*/
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
            }
        })
        /**
         * map 方法可以将　OnSubscribe 生成的事件在发送给 Consumer 或 Observer　之前做映射处理．
         * 例如将 Integer 转成 String
         * */
        .map(new Function<Integer, String>() {
            @Override
            public String apply(@NonNull Integer integer) throws Exception {
                return "This is result " + integer;
            }
        })
        /** Consumer 只有一个 accept 方法 */
        .subscribe(new Consumer<String>() {
            @Override
            public void accept(@NonNull String s) throws Exception {
                System.out.println("accept : " + s +"\n" );
            }
        });
    }

    @Test
    public void testZipTwoObservables() {
        /** 可以通过 Observable.zip 合并两个 Observable．最终配对出的 Observable 发射事件数目只和少的那个相同．*/

        Observable<String> o1 = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<String> e) throws Exception {
                if (!e.isDisposed()) {
                    System.out.println("Observable1 emit String: A");
                    e.onNext("A");
                    System.out.println("Observable1 emit String: B");
                    e.onNext("B");
                    System.out.println("Observable1 emit String: C");
                    e.onNext("C");
                }
            }
        });

        Observable<Integer> o2 = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
                if (!e.isDisposed()) {
                    System.out.println("Observable2 emit Integer : 1");
                    e.onNext(1);
                    System.out.println("Observable2 emit Integer : 2");
                    e.onNext(2);
                    System.out.println("Observable2 emit Integer : 3");
                    e.onNext(3);
                    System.out.println("Observable2 emit Integer : 4");
                    e.onNext(4);
                    System.out.println("Observable2 emit Integer : 5");
                    e.onNext(5);
                }
            }
        });

        /** 合并上面的两个 Observable *
         *
         * zip 只有一个 apply 方法, 返回一个 Observable
         */
        Observable.zip(o1, o2, new BiFunction<String, Integer, String>() {
            @Override
            public String apply(@NonNull String s, @NonNull Integer integer) throws Exception {
                return s + integer;
            }
        })
        /**
         * 合并上面的两个 Observable．合并行为是轮询执行的，而不是等到所有的 Observable 数据都接收到后才执行：
         *
         * (o1->o2->zip) => (o1->o2->zip) => ...
         *
         * 但是会随着短的 Observable 数据结束而结束．
         * */
        .subscribe(new Consumer<String>() {
            @Override
            public void accept(@NonNull String s) throws Exception {
                System.out.println("zip : accept : " + s);
            }
        });
    }

    @Test
    public void testContactObservable() {
        /** 和 zip 不同，concat 用于将若干个 observable 串联起来*/
        Observable.concat(
                Observable.just(1,2,3),
                Observable.just(4,5,6)
        ).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(@NonNull Integer integer) throws Exception {
                System.out.println("concat : "+ integer );
            }
        });
    }

    @Test
    public void testFlatMap() {
        /** FlatMap 可以把一个发射器  Observable 通过某种方法转换为多个 Observables，然后再把这些分散的 Observables装进一个
         * 单一的发射器 Observable。但有个需要注意的是，flatMap 并不能保证事件的顺序。*/
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
                /** 1)  fire 3 次 */
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
            }
            /** 2) flatMap 可以将数据映射到新的 Observable */
        }).flatMap((Function<Integer, ObservableSource<String>>) integer -> {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                list.add("I am value " + integer);
            }
            int delayTime = (int) (1 + Math.random() * 10);
            /** 2-1) 映射出三个新 Observable. 这三个新的 Observable 将会并发执行,只受 delayTime 的影响,不受 flatMap 接收到的顺序影响. */
            return Observable.fromIterable(list).delay(delayTime, TimeUnit.MILLISECONDS);
        }).subscribeOn(Schedulers.newThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(@NonNull String s) throws Exception {
                        System.out.println("flatMap : accept : " + s);
                    }
                });
    }

    @Test
    public void testConcatMap() {
        /** 与 flatMap 不同的是, concatMap return 的 Obserable 会依然保持接受到的顺序 */
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
            }
        }).concatMap((Function<Integer, ObservableSource<String>>) integer -> {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                list.add("I am value " + integer);
            }
            int delayTime = (int) (1 + Math.random() * 10);
            return Observable.fromIterable(list).delay(delayTime, TimeUnit.MILLISECONDS);
        }).subscribeOn(Schedulers.newThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(@NonNull String s) throws Exception {
                        System.out.println("flatMap : accept : " + s);
                    }
                });
    }
}

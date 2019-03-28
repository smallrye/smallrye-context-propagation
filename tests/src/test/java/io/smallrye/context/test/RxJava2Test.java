package io.smallrye.context.test;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.test.MyContext;

public class RxJava2Test {

    @BeforeClass
    public static void init() {
        // initialise
        SmallRyeContextManagerProvider.getManager();
    }

    @Before
    public void before() {
        MyContext.init();
        MyContext.get().set("test");
    }

    @After
    public void after() {
        MyContext.clear();
    }

    @Test
    public void testCompletable() throws Throwable {
        // check initial state
        checkContextCaptured();
        CountDownLatch latch = new CountDownLatch(1);

        Throwable[] ret = new Throwable[1];
        Completable.create(subscriber -> {
            // check deferred state
            checkContextCaptured();

            subscriber.onComplete();
        }).subscribeOn(Schedulers.newThread()).subscribe(() -> {
            latch.countDown();
        }, error -> {
            ret[0] = error;
            latch.countDown();
        });

        latch.await();
        if (ret[0] != null)
            throw ret[0];
    }

    @Test
    public void testSingle() throws Throwable {
        // check initial state
        checkContextCaptured();
        CountDownLatch latch = new CountDownLatch(1);

        Throwable[] ret = new Throwable[1];
        Single.create(subscriber -> {
            // check deferred state
            checkContextCaptured();

            subscriber.onSuccess("YES");
        }).subscribeOn(Schedulers.newThread()).subscribe(success -> {
            latch.countDown();
        }, error -> {
            ret[0] = error;
            latch.countDown();
        });

        latch.await();
        if (ret[0] != null)
            throw ret[0];
    }

    @Test
    public void testObservable() throws Throwable {
        // check initial state
        checkContextCaptured();
        CountDownLatch latch = new CountDownLatch(1);

        Throwable[] ret = new Throwable[1];
        Observable.create(emitter -> {
            // check deferred state
            checkContextCaptured();

            emitter.onNext("a");
            emitter.onComplete();
        }).subscribeOn(Schedulers.newThread()).subscribe(success -> {
            latch.countDown();
        }, error -> {
            ret[0] = error;
            latch.countDown();
        });

        latch.await();
        if (ret[0] != null)
            throw ret[0];
    }

    @Test
    public void testFlowable() throws Throwable {
        // check initial state
        checkContextCaptured();
        CountDownLatch latch = new CountDownLatch(1);

        Throwable[] ret = new Throwable[1];
        Flowable.create(emitter -> {
            // check deferred state
            checkContextCaptured();

            emitter.onNext("a");
            emitter.onComplete();
        }, BackpressureStrategy.BUFFER).subscribeOn(Schedulers.newThread()).subscribe(success -> {
            latch.countDown();
        }, error -> {
            ret[0] = error;
            latch.countDown();
        });

        latch.await();
        if (ret[0] != null)
            throw ret[0];
    }

    @Test
    public void testMaybe() throws Throwable {
        // check initial state
        checkContextCaptured();
        CountDownLatch latch = new CountDownLatch(1);

        Throwable[] ret = new Throwable[1];
        Maybe.create(emitter -> {
            // check deferred state
            checkContextCaptured();

            emitter.onSuccess("a");
            emitter.onComplete();
        }).subscribeOn(Schedulers.newThread()).subscribe(success -> {
            latch.countDown();
        }, error -> {
            ret[0] = error;
            latch.countDown();
        });

        latch.await();
        if (ret[0] != null)
            throw ret[0];
    }

    private void checkContextCaptured() {
        Assert.assertEquals("test", MyContext.get().getReqId());
    }
}

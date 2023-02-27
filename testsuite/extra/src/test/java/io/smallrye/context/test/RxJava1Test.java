package io.smallrye.context.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.test.util.AbstractTest;
import rx.Completable;
import rx.Emitter.BackpressureMode;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

class RxJava1Test extends AbstractTest {

    @BeforeAll
    static void init() {
        // initialise
        SmallRyeContextManagerProvider.getManager();
    }

    @BeforeEach
    void before() {
        MyContext.init();
        MyContext.get().set("test");
    }

    @AfterEach
    void after() {
        MyContext.clear();
    }

    @Test
    void completable() throws Throwable {
        // check initial state
        checkContextCaptured();
        CountDownLatch latch = new CountDownLatch(1);

        Throwable[] ret = new Throwable[1];
        Completable.create(subscriber -> {
            // check deferred state
            checkContextCaptured();

            subscriber.onCompleted();
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
    void single() throws Throwable {
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
    void observable() throws Throwable {
        // check initial state
        checkContextCaptured();
        CountDownLatch latch = new CountDownLatch(1);

        Throwable[] ret = new Throwable[1];
        Observable.create(emitter -> {
            // check deferred state
            checkContextCaptured();

            emitter.onNext("a");
            emitter.onCompleted();
        }, BackpressureMode.BUFFER).subscribeOn(Schedulers.newThread()).subscribe(success -> {
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
        assertEquals("test", MyContext.get().getReqId());
    }
}

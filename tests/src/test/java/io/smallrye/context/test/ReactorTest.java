package io.smallrye.context.test;

import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.test.util.AbstractTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CountDownLatch;

public class ReactorTest extends AbstractTest {

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
    public void testFlux() throws Throwable {
        // check initial state
        checkContextCaptured();

        CountDownLatch latch = new CountDownLatch(1);

        Throwable[] ret = new Throwable[1];
        Flux.create(sink -> {
                // check deferred state
                try {
                    checkContextCaptured();
                    sink.next("YES");
                    sink.complete();
                } catch (Throwable e) {
                    sink.error(e);
                }

            })
            .subscribeOn(Schedulers.newSingle("test"))
            .subscribe(
                success -> latch.countDown(),
                error -> {
                    ret[0] = error;
                    latch.countDown();
                });

        latch.await();

        if (ret[0] != null) {
            throw ret[0];
        }
    }

    @Test
    public void testMono() throws Throwable {
        // check initial state
        checkContextCaptured();

        CountDownLatch latch = new CountDownLatch(1);

        Throwable[] ret = new Throwable[1];

        Mono.create((sink) -> {
                try {
                    // check deferred state
                    checkContextCaptured();
                    sink.success("YES");
                } catch (Throwable e) {
                    sink.error(e);
                }
            })
            .subscribeOn(Schedulers.newSingle("test"))
            .subscribe(
                success -> latch.countDown(),
                e -> {
                    ret[0] = e;
                    latch.countDown();
                }
            );

        latch.await();

        if (ret[0] != null) {
            throw ret[0];
        }
    }

    private void checkContextCaptured() {
        Assert.assertEquals("test", MyContext.get().getReqId());
    }

}

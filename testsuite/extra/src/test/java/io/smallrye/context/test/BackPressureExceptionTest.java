package io.smallrye.context.test;

import static java.util.Arrays.asList;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.reactivex.observers.TestObserver;
import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.test.util.AbstractTest;
import rx.observers.AssertableSubscriber;

class BackPressureExceptionTest extends AbstractTest {

    @BeforeAll
    static void before() {
        // initialise
        SmallRyeContextManagerProvider.getManager();
    }

    @Test
    void backPressureRxJava1() {
        AssertableSubscriber<Integer> test = rx.Observable.from(asList(1, 2, 3, 4, 5, 6))
                .concatMap(integer -> rx.Observable.just(integer).delay(100, TimeUnit.MILLISECONDS))
                .test();

        test.awaitTerminalEvent();

        test.assertNoErrors();
        test.assertReceivedOnNext(asList(1, 2, 3, 4, 5, 6));
    }

    @Test
    void backPressureRxJava2() {
        TestObserver<Integer> test = io.reactivex.Observable.just(1, 2, 3, 4, 5, 6)
                .concatMap(integer -> io.reactivex.Observable.just(integer).delay(100, TimeUnit.MILLISECONDS))
                .test();

        test.awaitTerminalEvent();

        test.assertNoErrors();
        test.assertValues(1, 2, 3, 4, 5, 6);
    }
}

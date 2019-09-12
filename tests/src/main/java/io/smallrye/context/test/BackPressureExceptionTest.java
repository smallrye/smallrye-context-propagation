package io.smallrye.context.test;

import rx.observers.AssertableSubscriber;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

public class BackPressureExceptionTest {

    @BeforeClass
    public static void before() {
        Context.load();
    }


    @Test
    public void testBackPressureRxJava1() {
        AssertableSubscriber<Integer> test =
            rx.Observable.from(asList(1,2,3,4,5,6))
                .concatMap(integer -> rx.Observable.just(integer).delay(100, TimeUnit.MILLISECONDS))
                .test();

        test.awaitTerminalEvent();

        test.assertNoErrors();
        test.assertReceivedOnNext(asList(1,2,3,4,5,6));
    }

    @Test
    public void testBackPressureRxJava2() {
        TestObserver<Integer> test =
            io.reactivex.Observable.just(1,2,3,4,5,6)
                .concatMap(integer -> io.reactivex.Observable.just(integer).delay(100, TimeUnit.MILLISECONDS))
                .test();

        test.awaitTerminalEvent();

        test.assertNoErrors();
        test.assertValues(1,2,3,4,5,6);
    }
}

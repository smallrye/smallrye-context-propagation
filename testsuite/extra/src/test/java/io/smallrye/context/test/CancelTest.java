package io.smallrye.context.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.test.util.AbstractTest;

class CancelTest extends AbstractTest {
    @Test
    void cancelTest() throws InterruptedException, ExecutionException {
        CompletableFuture<String> cf = new CompletableFuture<>();
        Assertions.assertFalse(cf.isCancelled());
        Assertions.assertFalse(cf.isDone());
        Assertions.assertFalse(cf.isCompletedExceptionally());

        CountDownLatch cl = new CountDownLatch(1);
        CompletableFuture<String> cf2 = cf.whenComplete((val, x) -> {
            assertNull(val);
            Assertions.assertTrue(x instanceof CancellationException);
            cl.countDown();
        });

        // cancel the source CF
        Assertions.assertTrue(cf.cancel(true));

        Assertions.assertTrue(cf.isCancelled());
        Assertions.assertTrue(cf.isDone());
        Assertions.assertTrue(cf.isCompletedExceptionally());

        Assertions.assertFalse(cf2.isCancelled());
        Assertions.assertTrue(cf2.isDone());
        Assertions.assertTrue(cf2.isCompletedExceptionally());

        try {
            cf.get();
            Assertions.fail("Should have thrown");
        } catch (CancellationException x) {
            // This worked
        }

        try {
            cf2.get();
            Assertions.fail("Should have thrown");
        } catch (ExecutionException x) {
            Assertions.assertTrue(x.getCause() instanceof CancellationException);
        }

        // make sure the completion listener ran
        cl.await(5, TimeUnit.SECONDS);
    }

    @Test
    void cancelTestWrapped() throws InterruptedException, ExecutionException {
        SmallRyeThreadContext allTC = SmallRyeContextManagerProvider.getManager().allPropagatedThreadContext();

        CompletableFuture<String> cf = allTC.withContextCapture(new CompletableFuture<>());
        Assertions.assertFalse(cf.isCancelled());
        Assertions.assertFalse(cf.isDone());
        Assertions.assertFalse(cf.isCompletedExceptionally());

        CountDownLatch cl = new CountDownLatch(1);
        CompletableFuture<String> cf2 = cf.whenComplete((val, x) -> {
            assertNull(val);
            Assertions.assertTrue(x instanceof CancellationException);
            cl.countDown();
        });

        // cancel the source CF
        Assertions.assertTrue(cf.cancel(true));

        Assertions.assertTrue(cf.isCancelled());
        Assertions.assertTrue(cf.isDone());
        Assertions.assertTrue(cf.isCompletedExceptionally());

        Assertions.assertFalse(cf2.isCancelled());
        Assertions.assertTrue(cf2.isDone());
        Assertions.assertTrue(cf2.isCompletedExceptionally());

        try {
            cf.get();
            Assertions.fail("Should have thrown");
        } catch (CancellationException x) {
            // This worked
        }

        try {
            cf2.get();
            Assertions.fail("Should have thrown");
        } catch (ExecutionException x) {
            Assertions.assertTrue(x.getCause() instanceof CancellationException);
        }

        // make sure the completion listener ran
        cl.await(5, TimeUnit.SECONDS);
    }

    @Test
    void completeTest() throws InterruptedException, ExecutionException {
        CompletableFuture<String> cf = new CompletableFuture<>();
        Assertions.assertFalse(cf.isCancelled());
        Assertions.assertFalse(cf.isDone());
        Assertions.assertFalse(cf.isCompletedExceptionally());

        CountDownLatch cl = new CountDownLatch(1);
        CompletableFuture<String> cf2 = cf.whenComplete((val, x) -> {
            assertEquals("foo", val);
            Assertions.assertNull(x);
            cl.countDown();
        });

        // complete the source CF
        Assertions.assertTrue(cf.complete("foo"));

        Assertions.assertFalse(cf.isCancelled());
        Assertions.assertTrue(cf.isDone());
        Assertions.assertFalse(cf.isCompletedExceptionally());

        Assertions.assertFalse(cf2.isCancelled());
        Assertions.assertTrue(cf2.isDone());
        Assertions.assertFalse(cf2.isCompletedExceptionally());

        Assertions.assertEquals("foo", cf.get());
        Assertions.assertEquals("foo", cf2.get());

        // make sure the completion listener ran
        cl.await(5, TimeUnit.SECONDS);
    }

    @Test
    void completeTestWrapped() throws InterruptedException, ExecutionException {
        SmallRyeThreadContext allTC = SmallRyeContextManagerProvider.getManager().allPropagatedThreadContext();

        CompletableFuture<String> cf = allTC.withContextCapture(new CompletableFuture<>());
        Assertions.assertFalse(cf.isCancelled());
        Assertions.assertFalse(cf.isDone());
        Assertions.assertFalse(cf.isCompletedExceptionally());

        CountDownLatch cl = new CountDownLatch(1);
        CompletableFuture<String> cf2 = cf.whenComplete((val, x) -> {
            assertEquals("foo", val);
            Assertions.assertNull(x);
            cl.countDown();
        });

        // complete the source CF
        Assertions.assertTrue(cf.complete("foo"));

        Assertions.assertFalse(cf.isCancelled());
        Assertions.assertTrue(cf.isDone());
        Assertions.assertFalse(cf.isCompletedExceptionally());

        Assertions.assertFalse(cf2.isCancelled());
        Assertions.assertTrue(cf2.isDone());
        Assertions.assertFalse(cf2.isCompletedExceptionally());

        Assertions.assertEquals("foo", cf.get());
        Assertions.assertEquals("foo", cf2.get());

        // make sure the completion listener ran
        cl.await(5, TimeUnit.SECONDS);
    }

    @Test
    void completeTestDependent() throws InterruptedException, ExecutionException {
        CompletableFuture<String> cf = new CompletableFuture<>();
        Assertions.assertFalse(cf.isCancelled());
        Assertions.assertFalse(cf.isDone());
        Assertions.assertFalse(cf.isCompletedExceptionally());

        CountDownLatch cl = new CountDownLatch(1);
        CompletableFuture<String> cf2 = cf.whenComplete((val, x) -> {
            Assertions.fail("Should not have been called");
        });

        // complete the dependent CF
        Assertions.assertTrue(cf2.complete("foo"));

        Assertions.assertFalse(cf.isCancelled());
        Assertions.assertFalse(cf.isDone());
        Assertions.assertFalse(cf.isCompletedExceptionally());

        Assertions.assertFalse(cf2.isCancelled());
        Assertions.assertTrue(cf2.isDone());
        Assertions.assertFalse(cf2.isCompletedExceptionally());

        Assertions.assertThrows(TimeoutException.class, () -> cf.get(1, TimeUnit.SECONDS));
        Assertions.assertEquals("foo", cf2.get());

        // make sure the completion listener does not run
        cl.await(1, TimeUnit.SECONDS);
    }
}

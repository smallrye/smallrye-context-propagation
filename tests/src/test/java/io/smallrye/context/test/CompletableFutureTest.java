package io.smallrye.context.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.context.impl.Contextualized;

public class CompletableFutureTest {

    private ManagedExecutor managedExecutor;

    @Before
    public void before() {
        managedExecutor = ManagedExecutor.builder().build();
        MyContext.clear();
    }

    @Test
    public void testContextPropagation() throws InterruptedException, ExecutionException {
        // no context
        Assert.assertNull(MyContext.get());
        // nothing in the current thread
        managedExecutor.completedFuture(null)
                .thenApply(v -> {
                    Assert.assertNull(MyContext.get());
                    return v;
                })
                .get();
        // nothing in the executor thread
        managedExecutor.completedFuture(null)
                .thenApplyAsync(v -> {
                    Assert.assertNull(MyContext.get());
                    return v;
                })
                .get();
        // still no context
        Assert.assertNull(MyContext.get());

        // now with context
        MyContext ctx = new MyContext();
        MyContext.set(ctx);
        Assert.assertEquals(ctx, MyContext.get());
        // context in the current thread
        managedExecutor.completedFuture(null)
                .thenApply(v -> {
                    Assert.assertEquals(ctx, MyContext.get());
                    return v;
                })
                .get();
        // context in the executor thread
        managedExecutor.completedFuture(null)
                .thenApplyAsync(v -> {
                    Assert.assertEquals(ctx, MyContext.get());
                    return v;
                })
                .get();
        // still with context
        Assert.assertEquals(ctx, MyContext.get());

        // now create a CF while we have a context
        CompletableFuture<Void> cfCreatedWithContext = managedExecutor.completedFuture(null);
        // remove the context
        MyContext.clear();

        // check that we get no context, since we're capturing at lambda creation time
        // nothing in the current thread
        cfCreatedWithContext
                .thenApply(v -> {
                    Assert.assertNull(MyContext.get());
                    return v;
                })
                .get();
        // nothing in the executor thread
        cfCreatedWithContext
                .thenApplyAsync(v -> {
                    Assert.assertNull(MyContext.get());
                    return v;
                })
                .get();

        // now set it to another context and check we get the second context
        MyContext ctx2 = new MyContext();
        MyContext.set(ctx2);
        // context in the current thread
        cfCreatedWithContext
                .thenApply(v -> {
                    Assert.assertEquals(ctx2, MyContext.get());
                    return v;
                })
                .get();
        // context in the executor thread
        cfCreatedWithContext
                .thenApplyAsync(v -> {
                    Assert.assertEquals(ctx2, MyContext.get());
                    return v;
                })
                .get();
    }

    @Test
    public void testJava9Methods() throws InterruptedException, ExecutionException {
        // only run on JDK >= 9 which has that method
        try {
            CompletableFuture.class.getDeclaredMethod("copy");
        } catch (NoSuchMethodException | SecurityException e) {
            Assume.assumeTrue(false);
        }

        CompletableFuture<Object> cf = managedExecutor.completedFuture(null);
        // check that this new CF is not linked to the original CF
        CompletableFuture<Object> incomplete = cf.newIncompleteFuture();
        Assert.assertTrue(cf.isDone());
        Assert.assertFalse(incomplete.isDone());
        Assert.assertTrue(incomplete instanceof Contextualized);

        // check that the copy has the same completion as the original one
        CompletableFuture<Object> copy = cf.copy();
        Assert.assertTrue(copy.isDone());
        Assert.assertTrue(copy instanceof Contextualized);

        Assert.assertEquals(managedExecutor, cf.defaultExecutor());

        CompletionStage<Object> minimalCS = cf.minimalCompletionStage();
        // make sure we get in immediately since it's done just as the original cf
        String[] hack = new String[1];
        minimalCS.thenApply(v -> {
            Assert.assertNull(v);
            hack[0] = "OK";
            return v;
        });
        Assert.assertEquals("OK", hack[0]);
        Assert.assertTrue(minimalCS instanceof Contextualized);
        Assert.assertTrue(minimalCS instanceof CompletableFuture);
        Assert.assertThrows(UnsupportedOperationException.class, () -> ((CompletableFuture) minimalCS).complete(null));

        MyContext ctx = new MyContext();
        MyContext.set(ctx);
        CompletableFuture<Object> asyncCompleteCF = managedExecutor.newIncompleteFuture().completeAsync(() -> {
            Assert.assertEquals(ctx, MyContext.get());
            return null;
        });
        Assert.assertTrue(asyncCompleteCF instanceof Contextualized);
        Assert.assertNull(asyncCompleteCF.get());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        asyncCompleteCF = managedExecutor.newIncompleteFuture().completeAsync(() -> {
            Assert.assertEquals(ctx, MyContext.get());
            return null;
        }, executor);
        Assert.assertTrue(asyncCompleteCF instanceof Contextualized);
        Assert.assertNull(asyncCompleteCF.get());
        executor.awaitTermination(2, TimeUnit.SECONDS);

        CompletableFuture<Object> timeoutCF = managedExecutor.newIncompleteFuture().orTimeout(100, TimeUnit.MILLISECONDS);
        Assert.assertTrue(timeoutCF instanceof Contextualized);
        try {
            timeoutCF.get();
            Assert.fail();
        } catch (ExecutionException x) {
            Assert.assertTrue(x.getCause() instanceof TimeoutException);
        }

        timeoutCF = managedExecutor.newIncompleteFuture().completeOnTimeout(null, 100, TimeUnit.MILLISECONDS);
        Assert.assertTrue(timeoutCF instanceof Contextualized);
        Assert.assertNull(timeoutCF.get());
    }
}

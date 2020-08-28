package io.smallrye.context.test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.Test;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.SmallRyeThreadContext;

public class CurrentThreadContextTest {
    @Test
    public void testCurrentThreadContext() throws InterruptedException, ExecutionException {
        MyContext ctx = new MyContext();
        MyContext.set(ctx);
        Assert.assertEquals(ctx, MyContext.get());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            // all propagated contexts
            SmallRyeThreadContext allTC = SmallRyeContextManagerProvider.getManager().allPropagatedThreadContext();
            Assert.assertNull(SmallRyeThreadContext.getCurrentThreadContext());
            Runnable r = allTC.contextualRunnable(() -> {
                Assert.assertEquals(ctx, MyContext.get());
                Assert.assertEquals(allTC, SmallRyeThreadContext.getCurrentThreadContext());
            });
            executorService.submit(r).get();

            // all cleared contexts
            SmallRyeThreadContext noTC = SmallRyeContextManagerProvider.getManager().allClearedThreadContext();
            Assert.assertNull(SmallRyeThreadContext.getCurrentThreadContext());
            r = noTC.contextualRunnable(() -> {
                Assert.assertNull(MyContext.get());
                Assert.assertEquals(noTC, SmallRyeThreadContext.getCurrentThreadContext());
            });
            executorService.submit(r).get();

            // start with all, then change to no context
            r = allTC.contextualRunnable(() -> {
                // all TC
                Assert.assertEquals(ctx, MyContext.get());
                Assert.assertEquals(allTC, SmallRyeThreadContext.getCurrentThreadContext());
                try (CleanAutoCloseable ac = SmallRyeThreadContext.withThreadContext(noTC)) {
                    Assert.assertEquals(noTC, SmallRyeThreadContext.getCurrentThreadContext());
                    // no TC
                    Runnable r2 = SmallRyeThreadContext.getCurrentThreadContext().contextualRunnable(() -> {
                        Assert.assertNull(MyContext.get());
                        Assert.assertEquals(noTC, SmallRyeThreadContext.getCurrentThreadContext());
                    });
                    executorService.submit(r2).get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            executorService.submit(r).get();
        } finally {
            executorService.shutdownNow();
            MyContext.clear();
        }
    }
}

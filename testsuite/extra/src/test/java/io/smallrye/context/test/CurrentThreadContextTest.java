package io.smallrye.context.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.test.util.AbstractTest;

class CurrentThreadContextTest extends AbstractTest {
    @Test
    void currentThreadContext() throws InterruptedException, ExecutionException {
        MyContext ctx = new MyContext();
        MyContext.set(ctx);
        assertEquals(ctx, MyContext.get());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            // all propagated contexts
            SmallRyeThreadContext allTC = SmallRyeContextManagerProvider.getManager().allPropagatedThreadContext();
            assertNull(SmallRyeThreadContext.getCurrentThreadContext());
            Runnable r = allTC.contextualRunnable(() -> {
                assertEquals(ctx, MyContext.get());
                assertEquals(allTC, SmallRyeThreadContext.getCurrentThreadContext());
            });
            executorService.submit(r).get();

            // default contexts
            SmallRyeThreadContext defaultTC = SmallRyeContextManagerProvider.getManager().defaultThreadContext();
            assertNull(SmallRyeThreadContext.getCurrentThreadContext());
            r = defaultTC.contextualRunnable(() -> {
                assertEquals(ctx, MyContext.get());
                assertEquals(defaultTC, SmallRyeThreadContext.getCurrentThreadContext());
            });
            executorService.submit(r).get();

            // all cleared contexts
            SmallRyeThreadContext noTC = SmallRyeContextManagerProvider.getManager().allClearedThreadContext();
            assertNull(SmallRyeThreadContext.getCurrentThreadContext());
            r = noTC.contextualRunnable(() -> {
                assertNull(MyContext.get());
                assertEquals(noTC, SmallRyeThreadContext.getCurrentThreadContext());
            });
            executorService.submit(r).get();

            // start with all, then change to no context
            r = allTC.contextualRunnable(() -> {
                // all TC
                assertEquals(ctx, MyContext.get());
                assertEquals(allTC, SmallRyeThreadContext.getCurrentThreadContext());
                try (CleanAutoCloseable ac = SmallRyeThreadContext.withThreadContext(noTC)) {
                    assertEquals(noTC, SmallRyeThreadContext.getCurrentThreadContext());
                    // no TC
                    Runnable r2 = SmallRyeThreadContext.getCurrentThreadContext().contextualRunnable(() -> {
                        assertNull(MyContext.get());
                        assertEquals(noTC, SmallRyeThreadContext.getCurrentThreadContext());
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

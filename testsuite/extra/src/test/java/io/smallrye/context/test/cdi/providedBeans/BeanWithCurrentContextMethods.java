package io.smallrye.context.test.cdi.providedBeans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.api.CurrentThreadContext;
import io.smallrye.context.test.MyContext;

@ApplicationScoped
public class BeanWithCurrentContextMethods {

    @CurrentThreadContext
    public void assertCurrentContext() throws InterruptedException, ExecutionException {
        // default is to propagate everything
        SmallRyeThreadContext allTC = SmallRyeThreadContext.getCurrentThreadContext();
        assertNotNull(allTC);
        MyContext ctx = new MyContext();
        MyContext.set(ctx);
        assertEquals(ctx, MyContext.get());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            // all propagated contexts
            Runnable r = allTC.contextualRunnable(() -> {
                assertEquals(ctx, MyContext.get());
                assertEquals(allTC, SmallRyeThreadContext.getCurrentThreadContext());
            });
            executorService.submit(r).get();
        } finally {
            MyContext.clear();
        }
    }

    @CurrentThreadContext(propagated = {})
    public void assertCurrentContextAllCleared() throws InterruptedException, ExecutionException {
        // default is to propagate nothing
        SmallRyeThreadContext noTC = SmallRyeThreadContext.getCurrentThreadContext();
        assertNotNull(noTC);
        MyContext ctx = new MyContext();
        MyContext.set(ctx);
        assertEquals(ctx, MyContext.get());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            // no propagated contexts
            Runnable r = noTC.contextualRunnable(() -> {
                assertNull(MyContext.get());
                assertEquals(noTC, SmallRyeThreadContext.getCurrentThreadContext());
            });
            executorService.submit(r).get();
        } finally {
            MyContext.clear();
        }
    }

    @CurrentThreadContext(remove = true)
    public void assertCurrentContextRemoved() throws InterruptedException, ExecutionException {
        // no current context
        assertNull(SmallRyeThreadContext.getCurrentThreadContext());
    }

    public void assertNoCurrentContext() throws InterruptedException, ExecutionException {
        // no current context
        assertNull(SmallRyeThreadContext.getCurrentThreadContext());

        SmallRyeThreadContext clearedTC = SmallRyeContextManagerProvider.getManager().allClearedThreadContext();
        // make sure we can also remove it via the interceptor
        try (CleanAutoCloseable ac = SmallRyeThreadContext.withThreadContext(clearedTC)) {
            assertEquals(clearedTC, SmallRyeThreadContext.getCurrentThreadContext());
            // this.calls are not intercepted in Weld
            CDI.current().select(BeanWithCurrentContextMethods.class).get().assertCurrentContextRemoved();
        }
    }
}

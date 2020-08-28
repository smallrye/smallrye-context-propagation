package io.smallrye.context.test.cdi.providedBeans;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;

import org.junit.Assert;

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
        Assert.assertNotNull(allTC);
        MyContext ctx = new MyContext();
        MyContext.set(ctx);
        Assert.assertEquals(ctx, MyContext.get());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            // all propagated contexts
            Runnable r = allTC.contextualRunnable(() -> {
                Assert.assertEquals(ctx, MyContext.get());
                Assert.assertEquals(allTC, SmallRyeThreadContext.getCurrentThreadContext());
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
        Assert.assertNotNull(noTC);
        MyContext ctx = new MyContext();
        MyContext.set(ctx);
        Assert.assertEquals(ctx, MyContext.get());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            // no propagated contexts
            Runnable r = noTC.contextualRunnable(() -> {
                Assert.assertNull(MyContext.get());
                Assert.assertEquals(noTC, SmallRyeThreadContext.getCurrentThreadContext());
            });
            executorService.submit(r).get();
        } finally {
            MyContext.clear();
        }
    }

    @CurrentThreadContext(remove = true)
    public void assertCurrentContextRemoved() throws InterruptedException, ExecutionException {
        // no current context
        Assert.assertNull(SmallRyeThreadContext.getCurrentThreadContext());
    }

    public void assertNoCurrentContext() throws InterruptedException, ExecutionException {
        // no current context
        Assert.assertNull(SmallRyeThreadContext.getCurrentThreadContext());

        SmallRyeThreadContext clearedTC = SmallRyeContextManagerProvider.getManager().allClearedThreadContext();
        // make sure we can also remove it via the interceptor
        try (CleanAutoCloseable ac = SmallRyeThreadContext.withThreadContext(clearedTC)) {
            Assert.assertEquals(clearedTC, SmallRyeThreadContext.getCurrentThreadContext());
            // this.calls are not intercepted in Weld 
            CDI.current().select(BeanWithCurrentContextMethods.class).get().assertCurrentContextRemoved();
        }
    }
}

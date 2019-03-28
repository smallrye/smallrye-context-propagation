package io.smallrye.concurrency.test.cdi.context.propagation;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.enterprise.context.control.RequestContextController;
import javax.enterprise.inject.spi.CDI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CdiContextPropagatesTest {

    private static Weld weld;

    @BeforeClass
    public static void init() {
        // with smallrye-conc-cdi on CP, the CDI thread context provider gets
        // discovered
        weld = new Weld();
        weld.addBeanClasses(MyReqScopedBean.class);
    }

    @Test
    public void testRequestContextPropagates() throws InterruptedException {
        ManagedExecutor executor = ManagedExecutor.builder().maxAsync(2).propagated(ThreadContext.CDI).build();
        CountDownLatch latch = new CountDownLatch(1);

        int finalState = -1;
        try (WeldContainer container = weld.initialize()) {
            // activate request scope in SE, use the bean
            RequestContextController controller = container.select(RequestContextController.class).get();
            controller.activate();
            MyReqScopedBean reqScopedBean = container.select(MyReqScopedBean.class).get();
            reqScopedBean.incrementState();

            // run on executor
            executor.runAsync(() -> {
                CDI.current().select(MyReqScopedBean.class).get().incrementState();
                latch.countDown();
            });

            if (!latch.await(3, TimeUnit.SECONDS)) {
                Assert.fail("Waiting for CountDownLatch failed!");
            }
            finalState = reqScopedBean.getState();

            // finally, end request context and shutdown Weld container
            controller.deactivate();
        }
        // assert the state of the same bean was changed by another thread via
        // executor
        Assert.assertTrue(finalState == 2);
    }
}

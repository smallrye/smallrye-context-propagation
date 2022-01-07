package io.smallrye.context.test.cdi.context.propagation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.smallrye.context.test.JTAUtils;
import io.smallrye.context.test.util.AbstractTest;

public class CdiContextPropagatesTest extends AbstractTest {

    private static Weld weld;

    @BeforeClass
    public static void init() {
        JTAUtils.startJTATM();
        // with smallrye-conc-cdi on CP, the CDI thread context provider gets
        // discovered
        weld = new Weld();
        weld.addBeanClasses(MyReqScopedBean.class);
    }

    @AfterClass
    public static void stop() {
        JTAUtils.stop();
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

            if (!latch.await(20, TimeUnit.SECONDS)) {
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

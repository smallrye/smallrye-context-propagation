package io.smallrye.context.test.cdi.providedBeans;

import java.util.concurrent.ExecutionException;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.context.test.JTAUtils;

/**
 * Tests usage of {@link io.smallrye.context.api.WithCurrentContext}
 * on methods
 */
public class CurrentThreadContextInterceptorTest {

    @Before
    public void before() {
        // This is required because even if the JTA ThreadContextProvider can figure out that JTA is not available,
        // CDI will try to contact JTA via JNDI for its TransactionContext scope, which will break when propagating
        // the CDI context
        JTAUtils.startJTATM();
    }

    @After
    public void after() {
        JTAUtils.stop();
    }

    @Test
    public void testInterceptor() throws InterruptedException, ExecutionException {
        try (WeldContainer container = new Weld().addBeanClass(BeanWithCurrentContextMethods.class).initialize()) {
            BeanWithCurrentContextMethods bean = container.select(BeanWithCurrentContextMethods.class).get();
            bean.assertCurrentContext();
            bean.assertCurrentContextAllCleared();
            bean.assertNoCurrentContext();
        }
    }

}

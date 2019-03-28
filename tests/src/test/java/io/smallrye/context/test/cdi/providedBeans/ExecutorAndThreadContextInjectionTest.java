package io.smallrye.context.test.cdi.providedBeans;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Test;

/**
 * Tests usage of {@link io.smallrye.context.api.ManagedExecutorConfig}
 * and {@link io.smallrye.context.api.ThreadContextConfig} on injection points.
 *
 * @author Matej Novotny
 */
public class ExecutorAndThreadContextInjectionTest {

    @Test
    public void testInjectionWithConfigurationAndSharing() {
        try (WeldContainer container = new Weld().addBeanClass(BeanWithInjectionPoints.class).initialize()) {
            // being able to select the bean verifies that injection points are satisfied
            BeanWithInjectionPoints bean = container.select(BeanWithInjectionPoints.class).get();
            // since behind the scenes we use builders (which are tested in TCK), we only assert
            // state of the objects, e.g. correct amount of queue, async, contexts propagated,...
            bean.assertDefaultExecutor();
            bean.assertConfiguredManagedExecutor();
            bean.assertSharedExecutorsAreTheSame();

            bean.assertConfiguredThreadContext();
            bean.assertDefaultThreadContext();
            bean.assertSharedThreadContextsAreTheSame();
        }
    }

}

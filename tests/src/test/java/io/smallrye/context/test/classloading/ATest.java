package io.smallrye.context.test.classloading;

import java.util.List;
import java.util.function.BiConsumer;

import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.junit.Assert;

import io.smallrye.context.SmallRyeContextManager;
import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.impl.ThreadContextProviderPlan;

public class ATest implements BiConsumer<ClassLoader, ClassLoader> {

    @Override
    public void accept(ClassLoader thisClassLoader, ClassLoader parentClassLoader) {
        Assert.assertEquals(thisClassLoader, ATest.class.getClassLoader());
        ContextManagerProvider concurrencyProvider = ContextManagerProvider.instance();
        System.err.println("A CP: " + concurrencyProvider);
        System.err.println("A CM: " + concurrencyProvider.getContextManager());
        Assert.assertEquals(parentClassLoader, concurrencyProvider.getClass().getClassLoader());

        SmallRyeContextManager concurrencyManager = (SmallRyeContextManager) concurrencyProvider.getContextManager();
        ThreadContextProviderPlan plan = concurrencyManager.getProviderPlan();
        Assert.assertEquals(1, plan.propagatedProviders.size());
        Assert.assertEquals("A", plan.propagatedProviders.iterator().next().getThreadContextType());
        Assert.assertTrue(plan.unchangedProviders.isEmpty());
        Assert.assertTrue(plan.clearedProviders.isEmpty());

        List<ContextManagerExtension> propagators = SmallRyeContextManagerProvider.getManager().getExtensions();
        Assert.assertEquals(1, propagators.size());
        Assert.assertTrue(propagators.get(0).getClass() == MultiClassloadingTest.AThreadContextPropagator.class);
    }
}

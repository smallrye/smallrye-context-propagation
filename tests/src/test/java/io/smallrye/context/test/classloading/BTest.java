package io.smallrye.context.test.classloading;

import java.util.List;
import java.util.function.BiConsumer;

import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.junit.Assert;

import io.smallrye.context.SmallRyeContextManager;
import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.impl.ThreadContextProviderPlan;

public class BTest implements BiConsumer<ClassLoader, ClassLoader> {

    @Override
    public void accept(ClassLoader thisClassLoader, ClassLoader parentClassLoader) {
        Assert.assertEquals(thisClassLoader, BTest.class.getClassLoader());
        ContextManagerProvider contextProvider = ContextManagerProvider.instance();
        System.err.println("B CP: " + contextProvider);
        System.err.println("B CM: " + contextProvider.getContextManager());
        Assert.assertEquals(parentClassLoader, contextProvider.getClass().getClassLoader());

        SmallRyeContextManager contextManager = (SmallRyeContextManager) contextProvider.getContextManager();
        ThreadContextProviderPlan plan = contextManager.getProviderPlan();
        Assert.assertEquals(1, plan.propagatedProviders.size());
        Assert.assertEquals("B", plan.propagatedProviders.iterator().next().getThreadContextType());
        Assert.assertTrue(plan.unchangedProviders.isEmpty());
        Assert.assertTrue(plan.clearedProviders.isEmpty());

        List<ContextManagerExtension> propagators = SmallRyeContextManagerProvider.getManager().getExtensions();
        Assert.assertEquals(1, propagators.size());
        Assert.assertTrue(propagators.get(0).getClass() == MultiClassloadingTest.BThreadContextPropagator.class);
    }
}

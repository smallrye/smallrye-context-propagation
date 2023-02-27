package io.smallrye.context.test.classloading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.BiConsumer;

import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;

import io.smallrye.context.SmallRyeContextManager;
import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.impl.ThreadContextProviderPlan;

public class BTest implements BiConsumer<ClassLoader, ClassLoader> {

    @Override
    public void accept(ClassLoader thisClassLoader, ClassLoader parentClassLoader) {
        assertEquals(thisClassLoader, BTest.class.getClassLoader());
        ContextManagerProvider contextProvider = ContextManagerProvider.instance();
        System.err.println("B CP: " + contextProvider);
        System.err.println("B CM: " + contextProvider.getContextManager());
        assertEquals(parentClassLoader, contextProvider.getClass().getClassLoader());

        SmallRyeContextManager contextManager = (SmallRyeContextManager) contextProvider.getContextManager();
        ThreadContextProviderPlan plan = contextManager.getProviderPlan();
        assertEquals(1, plan.propagatedProviders.size());
        assertEquals("B", plan.propagatedProviders.iterator().next().getThreadContextType());
        assertTrue(plan.unchangedProviders.isEmpty());
        assertTrue(plan.clearedProviders.isEmpty());

        List<ContextManagerExtension> propagators = SmallRyeContextManagerProvider.getManager().getExtensions();
        assertEquals(1, propagators.size());
        assertSame(propagators.get(0).getClass(), MultiClassloadingTest.BThreadContextPropagator.class);
    }
}

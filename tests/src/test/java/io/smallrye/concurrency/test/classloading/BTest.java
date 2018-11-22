package io.smallrye.concurrency.test.classloading;

import java.util.List;
import java.util.function.BiConsumer;

import org.eclipse.microprofile.concurrent.spi.ConcurrencyProvider;
import org.junit.Assert;

import io.smallrye.concurrency.SmallRyeConcurrencyManager;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;
import io.smallrye.concurrency.impl.ThreadContextImpl;
import io.smallrye.concurrency.impl.ThreadContextProviderPlan;
import io.smallrye.concurrency.spi.ThreadContextPropagator;

public class BTest implements BiConsumer<ClassLoader,ClassLoader> {
    @Override
	public void accept(ClassLoader thisClassLoader, ClassLoader parentClassLoader) {
    	Assert.assertEquals(thisClassLoader, BTest.class.getClassLoader());
    	ConcurrencyProvider concurrencyProvider = ConcurrencyProvider.instance();
    	System.err.println("B CP: "+concurrencyProvider);
    	System.err.println("B CM: "+concurrencyProvider.getConcurrencyManager());
    	Assert.assertEquals(parentClassLoader, concurrencyProvider.getClass().getClassLoader());
    	
    	SmallRyeConcurrencyManager concurrencyManager = (SmallRyeConcurrencyManager) concurrencyProvider.getConcurrencyManager();
    	ThreadContextProviderPlan plan = concurrencyManager.getProviderPlan();
        Assert.assertEquals(1, plan.propagatedProviders.size());
        Assert.assertEquals("B", plan.propagatedProviders.iterator().next().getThreadContextType());
        Assert.assertTrue(plan.unchangedProviders.isEmpty());
        Assert.assertTrue(plan.clearedProviders.isEmpty());

        List<ThreadContextPropagator> propagators = SmallRyeConcurrencyProvider.getManager().getPropagators();
        Assert.assertEquals(1, propagators.size());
        Assert.assertTrue(propagators.get(0).getClass() == MultiClassloadingTest.BThreadContextPropagator.class);
    }
}
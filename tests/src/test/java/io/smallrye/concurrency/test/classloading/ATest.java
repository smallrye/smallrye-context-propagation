package io.smallrye.concurrency.test.classloading;

import java.util.List;
import java.util.function.BiConsumer;

import org.eclipse.microprofile.concurrent.spi.ConcurrencyProvider;
import org.junit.Assert;

import io.smallrye.concurrency.SmallRyeConcurrencyProvider;
import io.smallrye.concurrency.impl.ThreadContextImpl;
import io.smallrye.concurrency.spi.ThreadContextPropagator;

public class ATest implements BiConsumer<ClassLoader,ClassLoader> {
    @Override
	public void accept(ClassLoader thisClassLoader, ClassLoader parentClassLoader) {
    	Assert.assertEquals(thisClassLoader, ATest.class.getClassLoader());
    	ConcurrencyProvider concurrencyProvider = ConcurrencyProvider.instance();
    	System.err.println("A CP: "+concurrencyProvider);
    	System.err.println("A CM: "+concurrencyProvider.getConcurrencyManager());
    	Assert.assertEquals(parentClassLoader, concurrencyProvider.getClass().getClassLoader());
    	
        ThreadContextImpl threadContext = (ThreadContextImpl) concurrencyProvider.newThreadContextBuilder().build();
        Assert.assertArrayEquals(new String[] {"A"}, threadContext.getPropagated());
        Assert.assertArrayEquals(new String[] {}, threadContext.getUnchanged());
        
        List<ThreadContextPropagator> propagators = SmallRyeConcurrencyProvider.getManager().getPropagators();
        Assert.assertEquals(1, propagators.size());
        Assert.assertTrue(propagators.get(0).getClass() == MultiClassloadingTest.AThreadContextPropagator.class);
    }
}
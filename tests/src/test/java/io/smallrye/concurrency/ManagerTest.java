package io.smallrye.concurrency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.junit.Assert;
import org.junit.Test;

import io.smallrye.concurrency.impl.ThreadContextProviderPlan;
import io.smallrye.concurrency.test.DefaultThreadContextProvider;

public class ManagerTest {
	
	List<String> record = new ArrayList<>();
	
	ThreadContextProvider A = new DefaultThreadContextProvider("A", record);
	ThreadContextProvider B = new DefaultThreadContextProvider("B", record);
	
	@Test
	public void testContext() {
		SmallRyeConcurrencyManager manager = new SmallRyeConcurrencyManager(Arrays.asList(A, B), Collections.emptyList());

		// all providers
		ThreadContextProviderPlan providers = manager.getProviders();
		Assert.assertEquals(2, providers.propagatedProviders.size());
		Assert.assertTrue(providers.propagatedProviders.contains(A));
		Assert.assertTrue(providers.propagatedProviders.contains(B));
		Assert.assertEquals(0, providers.clearedProviders.size());

		// A propagated, B cleared, none unchanged
		providers = manager.getProviders(new String[] {"A"}, SmallRyeConcurrencyManager.NO_STRING, SmallRyeConcurrencyManager.ALL_REMAINING_ARRAY);
		Assert.assertEquals(1, providers.propagatedProviders.size());
		Assert.assertTrue(providers.propagatedProviders.contains(A));
		Assert.assertEquals(1, providers.clearedProviders.size());
		Assert.assertTrue(providers.clearedProviders.contains(B));

		// A propagated, none cleared, B unchanged
		providers = manager.getProviders(new String[] {"A"}, new String[] {"B"}, SmallRyeConcurrencyManager.NO_STRING);
		Assert.assertEquals(1, providers.propagatedProviders.size());
		Assert.assertTrue(providers.propagatedProviders.contains(A));
		Assert.assertEquals(0, providers.clearedProviders.size());

		// none propagated, A,B cleared, none unchanged
		providers = manager.getProviders(new String[] {}, new String[0], SmallRyeConcurrencyManager.ALL_REMAINING_ARRAY);
		Assert.assertEquals(0, providers.propagatedProviders.size());
		Assert.assertEquals(2, providers.clearedProviders.size());
		Assert.assertTrue(providers.clearedProviders.contains(A));
		Assert.assertTrue(providers.clearedProviders.contains(B));

		// none propagated, A cleared, B unchanged
		providers = manager.getProviders(new String[] {}, new String[] {"B"}, SmallRyeConcurrencyManager.ALL_REMAINING_ARRAY);
		Assert.assertEquals(0, providers.propagatedProviders.size());
		Assert.assertEquals(1, providers.clearedProviders.size());
		Assert.assertTrue(providers.clearedProviders.contains(A));
	}
}

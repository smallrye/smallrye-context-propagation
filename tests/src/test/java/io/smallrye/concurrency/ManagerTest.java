package io.smallrye.concurrency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.junit.Assert;
import org.junit.Test;

import io.smallrye.concurrency.SmallRyeConcurrencyManager;
import io.smallrye.concurrency.impl.ThreadContextImpl;
import io.smallrye.concurrency.impl.ThreadContextProviderPlan;
import io.smallrye.concurrency.test.DefaultThreadContextProvider;

public class ManagerTest {
	
	List<String> record = new ArrayList<>();
	
	ThreadContextProvider A = new DefaultThreadContextProvider("A", record);
	ThreadContextProvider B = new DefaultThreadContextProvider("B", record);
	ThreadContextProvider DEPENDS_ON_A = new DefaultThreadContextProvider("DEPENDS_ON_A", record) {
		@Override
		public Set<String> getPrerequisites() {
			return Collections.singleton("A");
		}
	};
	
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
		providers = manager.getProviders(new String[] {"A"}, new String[0]);
		Assert.assertEquals(1, providers.propagatedProviders.size());
		Assert.assertTrue(providers.propagatedProviders.contains(A));
		Assert.assertEquals(1, providers.clearedProviders.size());
		Assert.assertTrue(providers.clearedProviders.contains(B));

		// A propagated, none cleared, B unchanged
		providers = manager.getProviders(new String[] {"A"}, new String[] {"B"});
		Assert.assertEquals(1, providers.propagatedProviders.size());
		Assert.assertTrue(providers.propagatedProviders.contains(A));
		Assert.assertEquals(0, providers.clearedProviders.size());

		// none propagated, A,B cleared, none unchanged
		providers = manager.getProviders(new String[] {}, new String[0]);
		Assert.assertEquals(0, providers.propagatedProviders.size());
		Assert.assertEquals(2, providers.clearedProviders.size());
		Assert.assertTrue(providers.clearedProviders.contains(A));
		Assert.assertTrue(providers.clearedProviders.contains(B));

		// none propagated, A cleared, B unchanged
		providers = manager.getProviders(new String[] {}, new String[] {"B"});
		Assert.assertEquals(0, providers.propagatedProviders.size());
		Assert.assertEquals(1, providers.clearedProviders.size());
		Assert.assertTrue(providers.clearedProviders.contains(A));
	}

	@Test
	public void testContextProviderPlan() {
		SmallRyeConcurrencyManager manager = new SmallRyeConcurrencyManager(Arrays.asList(A, B, DEPENDS_ON_A), Collections.emptyList());

		// all providers
		ThreadContextProviderPlan providers = manager.getProviders();
		Assert.assertEquals(3, providers.propagatedProviders.size());
		Assert.assertTrue(providers.propagatedProviders.contains(A));
		Assert.assertTrue(providers.propagatedProviders.contains(B));
		Assert.assertTrue(providers.propagatedProviders.contains(DEPENDS_ON_A));
		Assert.assertTrue(providers.propagatedProviders.indexOf(A) < providers.propagatedProviders.indexOf(DEPENDS_ON_A));
		Assert.assertEquals(0, providers.clearedProviders.size());

		// DEPENDS_ON_A, A(implicit) propagated, B cleared, none unchanged
		providers = manager.getProviders(new String[] {"DEPENDS_ON_A"}, new String[] {});
		Assert.assertEquals(2, providers.propagatedProviders.size());
		Assert.assertTrue(providers.propagatedProviders.contains(A));
		Assert.assertTrue(providers.propagatedProviders.contains(DEPENDS_ON_A));
		Assert.assertTrue(providers.propagatedProviders.indexOf(A) < providers.propagatedProviders.indexOf(DEPENDS_ON_A));
		Assert.assertEquals(1, providers.clearedProviders.size());
		Assert.assertTrue(providers.clearedProviders.contains(B));

		// none propagated, B cleared, DEPENDS_ON_A, A(implicit) unchanged
		providers = manager.getProviders(new String[] {}, new String[] {"DEPENDS_ON_A"});
		Assert.assertEquals(0, providers.propagatedProviders.size());
		Assert.assertEquals(1, providers.clearedProviders.size());
		Assert.assertTrue(providers.clearedProviders.contains(B));
	}

	@Test
	public void testContextExecutionOrder() {
		SmallRyeConcurrencyManager managerWithoutB = new SmallRyeConcurrencyManager(Arrays.asList(DEPENDS_ON_A, A), Collections.emptyList());
		SmallRyeConcurrencyManager managerWithB = new SmallRyeConcurrencyManager(Arrays.asList(DEPENDS_ON_A, A, B), Collections.emptyList());
		
		// all providers
		ThreadContextImpl context = new ThreadContextImpl(managerWithoutB, managerWithoutB.getAllProviderTypes(), SmallRyeConcurrencyManager.NO_STRING);
		record.clear();
		context.withCurrentContext(() -> { record.add("action"); }).run();
		Assert.assertEquals(Arrays.asList("current before: A",
				"current before: DEPENDS_ON_A",
				"action",
				"current after: DEPENDS_ON_A",
				"current after: A"), record);

		// DEPENDS_ON_A, A(implicit) propagated, B cleared, none unchanged
		context = new ThreadContextImpl(managerWithB, new String[] {"DEPENDS_ON_A"}, new String[] {});
		record.clear();
		context.withCurrentContext(() -> { record.add("action"); }).run();
		Assert.assertEquals(Arrays.asList("current before: A",
				"current before: DEPENDS_ON_A",
				"default before: B",
				"action",
				"default after: B",
				"current after: DEPENDS_ON_A",
				"current after: A"), record);

		// none propagated, B cleared, DEPENDS_ON_A, A(implicit) unchanged
		context = new ThreadContextImpl(managerWithB, new String[] {}, new String[] {"DEPENDS_ON_A"});
		record.clear();
		context.withCurrentContext(() -> { record.add("action"); }).run();
		Assert.assertEquals(Arrays.asList("default before: B",
				"action",
				"default after: B"), record);

		// none propagated, A, DEPENDS_ON_A, B cleared, none unchanged
		context = new ThreadContextImpl(managerWithoutB, SmallRyeConcurrencyManager.NO_STRING, SmallRyeConcurrencyManager.NO_STRING);
		record.clear();
		context.withCurrentContext(() -> { record.add("action"); }).run();
		Assert.assertEquals(Arrays.asList("default before: A",
				"default before: DEPENDS_ON_A",
				"action",
				"default after: DEPENDS_ON_A",
				"default after: A"), record);
	}
}

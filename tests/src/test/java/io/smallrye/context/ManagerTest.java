package io.smallrye.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.junit.Assert;
import org.junit.Test;

import io.smallrye.context.impl.ThreadContextProviderPlan;
import io.smallrye.context.test.DefaultThreadContextProvider;

public class ManagerTest {

    List<String> record = new ArrayList<>();

    ThreadContextProvider A = new DefaultThreadContextProvider("A", record);

    ThreadContextProvider B = new DefaultThreadContextProvider("B", record);

    @Test
    public void testContext() {
        SmallRyeContextManager manager = new SmallRyeContextManager(Arrays.asList(A, B), Collections.emptyList(), null, false,
                null);

        // all providers
        ThreadContextProviderPlan providers = manager.getProviderPlan();
        Assert.assertEquals(2, providers.propagatedProviders.size());
        Assert.assertTrue(providers.propagatedProviders.contains(A));
        Assert.assertTrue(providers.propagatedProviders.contains(B));
        Assert.assertEquals(0, providers.clearedProviders.size());

        // A propagated, B cleared, none unchanged
        providers = manager.getProviderPlan(new String[] { "A" }, SmallRyeContextManager.NO_STRING,
                SmallRyeContextManager.ALL_REMAINING_ARRAY);
        Assert.assertEquals(1, providers.propagatedProviders.size());
        Assert.assertTrue(providers.propagatedProviders.contains(A));
        Assert.assertEquals(1, providers.clearedProviders.size());
        Assert.assertTrue(providers.clearedProviders.contains(B));

        // A propagated, none cleared, B unchanged
        providers = manager.getProviderPlan(new String[] { "A" }, new String[] { "B" }, SmallRyeContextManager.NO_STRING);
        Assert.assertEquals(1, providers.propagatedProviders.size());
        Assert.assertTrue(providers.propagatedProviders.contains(A));
        Assert.assertEquals(0, providers.clearedProviders.size());

        // none propagated, A,B cleared, none unchanged
        providers = manager.getProviderPlan(new String[] {}, new String[0], SmallRyeContextManager.ALL_REMAINING_ARRAY);
        Assert.assertEquals(0, providers.propagatedProviders.size());
        Assert.assertEquals(2, providers.clearedProviders.size());
        Assert.assertTrue(providers.clearedProviders.contains(A));
        Assert.assertTrue(providers.clearedProviders.contains(B));

        // none propagated, A cleared, B unchanged
        providers = manager.getProviderPlan(new String[] {}, new String[] { "B" }, SmallRyeContextManager.ALL_REMAINING_ARRAY);
        Assert.assertEquals(0, providers.propagatedProviders.size());
        Assert.assertEquals(1, providers.clearedProviders.size());
        Assert.assertTrue(providers.clearedProviders.contains(A));
    }
}

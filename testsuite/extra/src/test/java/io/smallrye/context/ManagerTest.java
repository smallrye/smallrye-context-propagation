package io.smallrye.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.junit.jupiter.api.Test;

import io.smallrye.context.impl.ThreadContextProviderPlan;
import io.smallrye.context.test.DefaultThreadContextProvider;
import io.smallrye.context.test.util.AbstractTest;

class ManagerTest extends AbstractTest {

    List<String> record = new ArrayList<>();

    ThreadContextProvider A = new DefaultThreadContextProvider("A", record);

    ThreadContextProvider B = new DefaultThreadContextProvider("B", record);

    @Test
    void testContext() {
        SmallRyeContextManager manager = new SmallRyeContextManager(Arrays.asList(A, B), Collections.emptyList(), null, false,
                null, true, null);

        // all providers
        ThreadContextProviderPlan providers = manager.getProviderPlan();
        assertEquals(2, providers.propagatedProviders.size());
        assertTrue(providers.propagatedProviders.contains(A));
        assertTrue(providers.propagatedProviders.contains(B));
        assertEquals(0, providers.clearedProviders.size());

        // A propagated, B cleared, none unchanged
        providers = manager.getProviderPlan(new String[] { "A" }, SmallRyeContextManager.NO_STRING,
                SmallRyeContextManager.ALL_REMAINING_ARRAY);
        assertEquals(1, providers.propagatedProviders.size());
        assertTrue(providers.propagatedProviders.contains(A));
        assertEquals(1, providers.clearedProviders.size());
        assertTrue(providers.clearedProviders.contains(B));

        // A propagated, none cleared, B unchanged
        providers = manager.getProviderPlan(new String[] { "A" }, new String[] { "B" }, SmallRyeContextManager.NO_STRING);
        assertEquals(1, providers.propagatedProviders.size());
        assertTrue(providers.propagatedProviders.contains(A));
        assertEquals(0, providers.clearedProviders.size());

        // none propagated, A,B cleared, none unchanged
        providers = manager.getProviderPlan(new String[] {}, new String[0], SmallRyeContextManager.ALL_REMAINING_ARRAY);
        assertEquals(0, providers.propagatedProviders.size());
        assertEquals(2, providers.clearedProviders.size());
        assertTrue(providers.clearedProviders.contains(A));
        assertTrue(providers.clearedProviders.contains(B));

        // none propagated, A cleared, B unchanged
        providers = manager.getProviderPlan(new String[] {}, new String[] { "B" }, SmallRyeContextManager.ALL_REMAINING_ARRAY);
        assertEquals(0, providers.propagatedProviders.size());
        assertEquals(1, providers.clearedProviders.size());
        assertTrue(providers.clearedProviders.contains(A));
    }
}

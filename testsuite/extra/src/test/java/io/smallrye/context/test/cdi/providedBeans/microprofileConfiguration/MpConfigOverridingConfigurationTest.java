package io.smallrye.context.test.cdi.providedBeans.microprofileConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.Test;

import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.impl.ThreadContextProviderPlan;
import io.smallrye.context.test.cdi.providedBeans.Utils;

class MpConfigOverridingConfigurationTest {

    @Test
    void overridingIPsWithMpConfig() {
        try (WeldContainer container = new Weld().addBeanClass(SomeBean.class).initialize()) {
            SomeBean bean = container.select(SomeBean.class).get();
            // verify standard field IPs
            verifyManagedExecutor(Utils.unwrapExecutor(bean.getOverrideDefaultME()));
            verifyManagedExecutor(Utils.unwrapExecutor(bean.getOverrideConfiguredME()));
            verifyThreadContext(Utils.unwrapThreadContext(bean.getOverrideDefaultTC()));
            verifyThreadContext(Utils.unwrapThreadContext(bean.getOverrideConfiguredTC()));
            // verify IP as method parameter where we set empty value via MP config
            container.event().select(String.class).fire("foo");
            assertTrue(SomeBean.OBSERVER_NOTIFIED);
            ThreadContextProviderPlan plan = Utils.unwrapExecutor(bean.getExecutorFromObserverMethod())
                    .getThreadContextProviderPlan();
            Set<String> propagated = Utils.providersToStringSet(plan.propagatedProviders);
            assertTrue(propagated.contains(ThreadContext.TRANSACTION));
            assertTrue(propagated.contains(ThreadContext.CDI));
            assertEquals(0, plan.clearedProviders.size());
            assertEquals(0, plan.unchangedProviders.size());
        }
    }

    private void verifyManagedExecutor(SmallRyeManagedExecutor me) {
        assertEquals(-1, me.getMaxQueued());
        // max async is overriden by MP config
        assertEquals(2, me.getMaxAsync());
        ThreadContextProviderPlan threadContextProviderPlan = me.getThreadContextProviderPlan();
        // CDI context is moved to cleared contexts by MP config
        assertEquals(1, threadContextProviderPlan.clearedProviders.size());
        assertEquals(ThreadContext.CDI,
                threadContextProviderPlan.clearedProviders.iterator().next().getThreadContextType());
        // indirectly verify that propagated contained all remaining, e.g. transactions will be there
        Set<String> propagated = Utils.providersToStringSet(threadContextProviderPlan.propagatedProviders);
        assertTrue(propagated.contains(ThreadContext.TRANSACTION));
        assertEquals(0, threadContextProviderPlan.unchangedProviders.size());
    }

    private void verifyThreadContext(SmallRyeThreadContext tc) {
        ThreadContextProviderPlan threadContextProviderPlan = tc.getPlan();
        // CDI context is moved to cleared contexts by MP config
        assertEquals(1, threadContextProviderPlan.clearedProviders.size());
        assertEquals(ThreadContext.CDI,
                threadContextProviderPlan.clearedProviders.iterator().next().getThreadContextType());
        // indirectly verify that propagated contained all remaining, e.g. transactions will be there
        Set<String> propagated = Utils.providersToStringSet(threadContextProviderPlan.propagatedProviders);
        assertTrue(propagated.contains(ThreadContext.TRANSACTION));
        assertEquals(0, threadContextProviderPlan.unchangedProviders.size());
    }
}

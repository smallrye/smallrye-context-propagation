package io.smallrye.context.test.cdi.providedBeans.microprofileConfiguration;

import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.impl.ThreadContextProviderPlan;
import io.smallrye.context.test.cdi.providedBeans.Utils;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class MpConfigOverridingConfigurationTest {

    @Test
    public void testOverridingIPsWithMpConfig() {
        try (WeldContainer container = new Weld().addBeanClass(SomeBean.class).initialize()) {
            SomeBean bean = container.select(SomeBean.class).get();
            // verify standard field IPs
            verifyManagedExecutor(Utils.unwrapExecutor(bean.getOverrideDefaultME()));
            verifyManagedExecutor(Utils.unwrapExecutor(bean.getOverrideConfiguredME()));
            verifyThreadContext(Utils.unwrapThreadContext(bean.getOverrideDefaultTC()));
            verifyThreadContext(Utils.unwrapThreadContext(bean.getOverrideConfiguredTC()));
            // verify IP as method parameter where we set empty value via MP config
            container.event().select(String.class).fire("foo");
            Assert.assertTrue(SomeBean.OBSERVER_NOTIFIED);
            ThreadContextProviderPlan plan = Utils.unwrapExecutor(bean.getExecutorFromObserverMethod()).getThreadContextProviderPlan();
            Set<String> propagated = Utils.providersToStringSet(plan.propagatedProviders);
            Assert.assertTrue(propagated.contains(ThreadContext.TRANSACTION));
            Assert.assertTrue(propagated.contains(ThreadContext.CDI));
            Assert.assertTrue(plan.clearedProviders.size() == 0);
            Assert.assertTrue(plan.unchangedProviders.size() == 0);
        }
    }

    private void verifyManagedExecutor(SmallRyeManagedExecutor me) {
        Assert.assertEquals(-1, me.getMaxQueued());
        // max async is overriden by MP config
        Assert.assertEquals(2, me.getMaxAsync());
        ThreadContextProviderPlan threadContextProviderPlan = me.getThreadContextProviderPlan();
        // CDI context is moved to cleared contexts by MP config
        Assert.assertTrue(threadContextProviderPlan.clearedProviders.size() == 1);
        Assert.assertEquals(ThreadContext.CDI, threadContextProviderPlan.clearedProviders.iterator().next().getThreadContextType());
        // indirectly verify that propagated contained all remaining, e.g. transactions will be there
        Set<String> propagated = Utils.providersToStringSet(threadContextProviderPlan.propagatedProviders);
        Assert.assertTrue(propagated.contains(ThreadContext.TRANSACTION));
        Assert.assertTrue(threadContextProviderPlan.unchangedProviders.size() == 0);
    }

    private void verifyThreadContext(SmallRyeThreadContext tc) {
        ThreadContextProviderPlan threadContextProviderPlan = tc.getPlan();
        // CDI context is moved to cleared contexts by MP config
        Assert.assertTrue(threadContextProviderPlan.clearedProviders.size() == 1);
        Assert.assertEquals(ThreadContext.CDI, threadContextProviderPlan.clearedProviders.iterator().next().getThreadContextType());
        // indirectly verify that propagated contained all remaining, e.g. transactions will be there
        Set<String> propagated = Utils.providersToStringSet(threadContextProviderPlan.propagatedProviders);
        Assert.assertTrue(propagated.contains(ThreadContext.TRANSACTION));
        Assert.assertTrue(threadContextProviderPlan.unchangedProviders.size() == 0);
    }
}

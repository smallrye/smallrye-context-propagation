package io.smallrye.context.test.cdi.providedBeans;

import static io.smallrye.context.test.cdi.providedBeans.Utils.providersToStringSet;
import static io.smallrye.context.test.cdi.providedBeans.Utils.unwrapExecutor;
import static io.smallrye.context.test.cdi.providedBeans.Utils.unwrapThreadContext;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.Assert;

import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.api.ManagedExecutorConfig;
import io.smallrye.context.api.NamedInstance;
import io.smallrye.context.api.ThreadContextConfig;
import io.smallrye.context.impl.ThreadContextProviderPlan;

/**
 * There are multiple context providers added in tests, we do not assert on those and only look for CDI and JTA now.
 */
@ApplicationScoped
public class BeanWithInjectionPoints {

    @Inject
    @ManagedExecutorConfig
    ManagedExecutor defaultConfigExecutor;

    @Inject
    @ManagedExecutorConfig
    @NamedInstance("sharedDefaultExecutor")
    ManagedExecutor executor2;

    @Inject
    @NamedInstance("sharedDefaultExecutor")
    ManagedExecutor executor3;

    @Inject
    @ManagedExecutorConfig(maxAsync = 2, maxQueued = 3, cleared = ThreadContext.CDI, propagated = ThreadContext.TRANSACTION)
    ManagedExecutor configuredExecutor;

    @Inject
    @ThreadContextConfig
    ThreadContext defaultThreadContext;

    @Inject
    @ThreadContextConfig
    @NamedInstance("sharedThreadContext")
    ThreadContext threadContext1;

    @Inject
    @NamedInstance("sharedThreadContext")
    ThreadContext threadContext2;

    @Inject
    @ThreadContextConfig(propagated = ThreadContext.CDI, cleared = ThreadContext.TRANSACTION, unchanged = "")
    ThreadContext configuredThreadContext;

    public void assertDefaultExecutor() {
        SmallRyeManagedExecutor exec = unwrapExecutor(defaultConfigExecutor);
        Assert.assertEquals(-1, exec.getMaxAsync());
        Assert.assertEquals(-1, exec.getMaxQueued());
        ThreadContextProviderPlan plan = exec.getThreadContextProviderPlan();
        Assert.assertEquals(0, plan.unchangedProviders.size());
        Assert.assertEquals(0, plan.clearedProviders.size());
        Set<String> propagated = providersToStringSet(plan.propagatedProviders);
        Assert.assertTrue(propagated.contains(ThreadContext.CDI));
        Assert.assertTrue(propagated.contains(ThreadContext.TRANSACTION));
    }

    public void assertSharedExecutorsAreTheSame() {
        // simply unwrap both and compare reference
        SmallRyeManagedExecutor shared1 = unwrapExecutor(executor2);
        SmallRyeManagedExecutor shared2 = unwrapExecutor(executor3);
        Assert.assertSame(shared1, shared2);
    }

    public void assertConfiguredManagedExecutor() {
        SmallRyeManagedExecutor exec = unwrapExecutor(configuredExecutor);
        Assert.assertEquals(2, exec.getMaxAsync());
        Assert.assertEquals(3, exec.getMaxQueued());
        ThreadContextProviderPlan plan = exec.getThreadContextProviderPlan();
        Assert.assertEquals(0, plan.unchangedProviders.size());
        Set<String> propagated = providersToStringSet(plan.propagatedProviders);
        Set<String> cleared = providersToStringSet(plan.clearedProviders);
        Assert.assertTrue(cleared.contains(ThreadContext.CDI));
        Assert.assertTrue(propagated.contains(ThreadContext.TRANSACTION));
    }

    public void assertSharedThreadContextsAreTheSame() {
        //unwrap and compare references
        SmallRyeThreadContext shared1 = unwrapThreadContext(threadContext1);
        SmallRyeThreadContext shared2 = unwrapThreadContext(threadContext2);
        Assert.assertSame(shared1, shared2);
    }

    public void assertConfiguredThreadContext() {
        SmallRyeThreadContext context = unwrapThreadContext(configuredThreadContext);
        ThreadContextProviderPlan plan = context.getPlan();
        Assert.assertEquals(0, plan.unchangedProviders.size());
        Set<String> propagated = providersToStringSet(plan.propagatedProviders);
        Set<String> cleared = providersToStringSet(plan.clearedProviders);
        Assert.assertTrue(propagated.contains(ThreadContext.CDI));
        Assert.assertTrue(cleared.contains(ThreadContext.TRANSACTION));
    }

    public void assertDefaultThreadContext() {
        SmallRyeThreadContext context = unwrapThreadContext(defaultThreadContext);
        ThreadContextProviderPlan plan = context.getPlan();
        Assert.assertEquals(0, plan.unchangedProviders.size());
        Assert.assertEquals(0, plan.clearedProviders.size());
        Set<String> propagated = providersToStringSet(plan.propagatedProviders);
        Assert.assertTrue(propagated.contains(ThreadContext.CDI));
        Assert.assertTrue(propagated.contains(ThreadContext.TRANSACTION));
    }
}

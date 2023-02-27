package io.smallrye.context.test.cdi.providedBeans;

import static io.smallrye.context.test.cdi.providedBeans.Utils.providersToStringSet;
import static io.smallrye.context.test.cdi.providedBeans.Utils.unwrapExecutor;
import static io.smallrye.context.test.cdi.providedBeans.Utils.unwrapThreadContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

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
        assertEquals(-1, exec.getMaxAsync());
        assertEquals(-1, exec.getMaxQueued());
        ThreadContextProviderPlan plan = exec.getThreadContextProviderPlan();
        assertEquals(0, plan.unchangedProviders.size());
        assertEquals(0, plan.clearedProviders.size());
        Set<String> propagated = providersToStringSet(plan.propagatedProviders);
        assertTrue(propagated.contains(ThreadContext.CDI));
        assertTrue(propagated.contains(ThreadContext.TRANSACTION));
    }

    public void assertSharedExecutorsAreTheSame() {
        // simply unwrap both and compare reference
        SmallRyeManagedExecutor shared1 = unwrapExecutor(executor2);
        SmallRyeManagedExecutor shared2 = unwrapExecutor(executor3);
        assertSame(shared1, shared2);
    }

    public void assertConfiguredManagedExecutor() {
        SmallRyeManagedExecutor exec = unwrapExecutor(configuredExecutor);
        assertEquals(2, exec.getMaxAsync());
        assertEquals(3, exec.getMaxQueued());
        ThreadContextProviderPlan plan = exec.getThreadContextProviderPlan();
        assertEquals(0, plan.unchangedProviders.size());
        Set<String> propagated = providersToStringSet(plan.propagatedProviders);
        Set<String> cleared = providersToStringSet(plan.clearedProviders);
        assertTrue(cleared.contains(ThreadContext.CDI));
        assertTrue(propagated.contains(ThreadContext.TRANSACTION));
    }

    public void assertSharedThreadContextsAreTheSame() {
        //unwrap and compare references
        SmallRyeThreadContext shared1 = unwrapThreadContext(threadContext1);
        SmallRyeThreadContext shared2 = unwrapThreadContext(threadContext2);
        assertSame(shared1, shared2);
    }

    public void assertConfiguredThreadContext() {
        SmallRyeThreadContext context = unwrapThreadContext(configuredThreadContext);
        ThreadContextProviderPlan plan = context.getPlan();
        assertEquals(0, plan.unchangedProviders.size());
        Set<String> propagated = providersToStringSet(plan.propagatedProviders);
        Set<String> cleared = providersToStringSet(plan.clearedProviders);
        assertTrue(propagated.contains(ThreadContext.CDI));
        assertTrue(cleared.contains(ThreadContext.TRANSACTION));
    }

    public void assertDefaultThreadContext() {
        SmallRyeThreadContext context = unwrapThreadContext(defaultThreadContext);
        ThreadContextProviderPlan plan = context.getPlan();
        assertEquals(0, plan.unchangedProviders.size());
        assertEquals(0, plan.clearedProviders.size());
        Set<String> propagated = providersToStringSet(plan.propagatedProviders);
        assertTrue(propagated.contains(ThreadContext.CDI));
        assertTrue(propagated.contains(ThreadContext.TRANSACTION));
    }
}

package io.smallrye.context.test.cdi.providedBeans;

import io.smallrye.context.api.ManagedExecutorConfig;
import io.smallrye.context.api.NamedInstance;
import io.smallrye.context.api.ThreadContextConfig;
import io.smallrye.context.impl.ManagedExecutorImpl;
import io.smallrye.context.impl.ThreadContextImpl;
import io.smallrye.context.impl.ThreadContextProviderPlan;
import io.vertx.core.cli.annotations.Name;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.hibernate.engine.spi.Managed;
import org.jboss.weld.proxy.WeldClientProxy;
import org.junit.Assert;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

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
        ManagedExecutorImpl exec = unwrapExecutor(defaultConfigExecutor);
        Assert.assertEquals(-1,exec.getMaxAsync());
        Assert.assertEquals(-1,exec.getMaxQueued());
        ThreadContextProviderPlan plan = exec.getThreadContextProviderPlan();
        Assert.assertEquals(0,plan.unchangedProviders.size());
        Assert.assertEquals(0,plan.clearedProviders.size());
        Set<String> propagated = providersToStringSet(plan.propagatedProviders);
        Assert.assertTrue(propagated.contains(ThreadContext.CDI));
        Assert.assertTrue(propagated.contains(ThreadContext.TRANSACTION));
    }

    public void assertSharedExecutorsAreTheSame() {
        // simply unwrap both and compare reference
        ManagedExecutorImpl shared1 = unwrapExecutor(executor2);
        ManagedExecutorImpl shared2 = unwrapExecutor(executor3);
        Assert.assertSame(shared1, shared2);
    }

    public void assertConfiguredManagedExecutor() {
        ManagedExecutorImpl exec = unwrapExecutor(configuredExecutor);
        Assert.assertEquals(2,exec.getMaxAsync());
        Assert.assertEquals(3,exec.getMaxQueued());
        ThreadContextProviderPlan plan = exec.getThreadContextProviderPlan();
        Assert.assertEquals(0,plan.unchangedProviders.size());
        Set<String> propagated = providersToStringSet(plan.propagatedProviders);
        Set<String> cleared = providersToStringSet(plan.clearedProviders);
        Assert.assertTrue(cleared.contains(ThreadContext.CDI));
        Assert.assertTrue(propagated.contains(ThreadContext.TRANSACTION));
    }

    public void assertSharedThreadContextsAreTheSame() {
        //unwrap and compare references
        ThreadContextImpl shared1 = unwrapThreadContext(threadContext1);
        ThreadContextImpl shared2 = unwrapThreadContext(threadContext2);
        Assert.assertSame(shared1, shared2);
    }

    public void assertConfiguredThreadContext() {
        ThreadContextImpl context = unwrapThreadContext(configuredThreadContext);
        ThreadContextProviderPlan plan = context.getPlan();
        Assert.assertEquals(0,plan.unchangedProviders.size());
        Set<String> propagated = providersToStringSet(plan.propagatedProviders);
        Set<String> cleared = providersToStringSet(plan.clearedProviders);
        Assert.assertTrue(propagated.contains(ThreadContext.CDI));
        Assert.assertTrue(cleared.contains(ThreadContext.TRANSACTION));
    }

    public void assertDefaultThreadContext() {
        ThreadContextImpl context = unwrapThreadContext(defaultThreadContext);
        ThreadContextProviderPlan plan = context.getPlan();
        Assert.assertEquals(0,plan.unchangedProviders.size());
        Assert.assertEquals(0,plan.clearedProviders.size());
        Set<String> propagated = providersToStringSet(plan.propagatedProviders);
        Assert.assertTrue(propagated.contains(ThreadContext.CDI));
        Assert.assertTrue(propagated.contains(ThreadContext.TRANSACTION));
    }

    private ManagedExecutorImpl unwrapExecutor(ManagedExecutor executor) {
        if (executor instanceof WeldClientProxy) {
            return (ManagedExecutorImpl) ((WeldClientProxy) executor).getMetadata().getContextualInstance();
        } else {
            throw new IllegalStateException("Injected proxies are expected to be instance of WeldClientProxy");
        }
    }

    private ThreadContextImpl unwrapThreadContext(ThreadContext executor) {
        if (executor instanceof WeldClientProxy) {
            return (ThreadContextImpl) ((WeldClientProxy) executor).getMetadata().getContextualInstance();
        } else {
            throw new IllegalStateException("Injected proxies are expected to be instance of WeldClientProxy");
        }
    }

    private Set<String> providersToStringSet(Set<ThreadContextProvider> providers) {
        Set<String> result = new HashSet<>();
        for (ThreadContextProvider provider : providers) {
            result.add(provider.getThreadContextType());
        }
        return result;
    }
}

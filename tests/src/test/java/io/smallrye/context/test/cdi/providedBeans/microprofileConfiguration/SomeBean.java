package io.smallrye.context.test.cdi.providedBeans.microprofileConfiguration;

import io.smallrye.context.api.ManagedExecutorConfig;
import io.smallrye.context.api.NamedInstance;
import io.smallrye.context.api.ThreadContextConfig;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

/**
 * MP config overrides all the ME and TC injection points in some aspect.
 * Presence/absence of @NamedInstance shouldn't change the functionality.
 */
@ApplicationScoped
public class SomeBean {

    public static boolean OBSERVER_NOTIFIED = false;

    @Inject
    private ManagedExecutor overrideDefaultME;

    @Inject
    @NamedInstance("configuredME")
    @ManagedExecutorConfig(cleared = "", maxAsync = -1, maxQueued = -1, propagated = ThreadContext.ALL_REMAINING)
    private ManagedExecutor overrideConfiguredME;

    @Inject
    @NamedInstance("defaultTC")
    private ThreadContext overrideDefaultTC;

    @Inject
    @ThreadContextConfig(propagated = ThreadContext.ALL_REMAINING, cleared = "", unchanged = "")
    private ThreadContext overrideConfiguredTC;

    private ManagedExecutor executorFromObserverMethod;

    public ManagedExecutor getOverrideDefaultME() {
        return overrideDefaultME;
    }

    public ManagedExecutor getOverrideConfiguredME() {
        return overrideConfiguredME;
    }

    public ManagedExecutor getExecutorFromObserverMethod() {
        return executorFromObserverMethod;
    }

    public ThreadContext getOverrideDefaultTC() {
        return overrideDefaultTC;
    }

    public ThreadContext getOverrideConfiguredTC() {
        return overrideConfiguredTC;
    }

    public void observeSomething(@Observes String payload, @ManagedExecutorConfig(cleared = ThreadContext.CDI) ManagedExecutor parameterME) {
        OBSERVER_NOTIFIED = true;
        executorFromObserverMethod = parameterME;
    }
}

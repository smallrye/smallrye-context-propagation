package io.smallrye.context.impl;

import java.util.Collections;
import java.util.Set;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;

public class ThreadContextProviderPlan {

    public final Set<ThreadContextProvider> propagatedProviders;
    public final Set<ThreadContextProvider> unchangedProviders;
    public final Set<ThreadContextProvider> clearedProviders;

    public ThreadContextProviderPlan(Set<ThreadContextProvider> propagatedSet, Set<ThreadContextProvider> unchangedSet,
            Set<ThreadContextProvider> clearedSet) {
        this.propagatedProviders = Collections.unmodifiableSet(propagatedSet);
        this.unchangedProviders = Collections.unmodifiableSet(unchangedSet);
        this.clearedProviders = Collections.unmodifiableSet(clearedSet);
    }
}

package io.smallrye.context.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

public class ThreadContextProviderPlan {

    public final Set<ThreadContextProvider> propagatedProviders;
    public final Set<ThreadContextProvider> unchangedProviders;
    public final Set<ThreadContextProvider> clearedProviders;

    /**
     * Following 3 fields are meant to optimise method #takeThreadContextSnapshots
     * which is extremely hot at runtime, at cost of little extra memory for this plan.
     */
    private final int snapshotInitialSize;
    private final ThreadContextProvider[] propagatedProvidersFastIterable;
    private final ThreadContextProvider[] clearedProvidersFastIterable;

    public ThreadContextProviderPlan(Set<ThreadContextProvider> propagatedSet, Set<ThreadContextProvider> unchangedSet,
            Set<ThreadContextProvider> clearedSet) {
        this.propagatedProviders = Collections.unmodifiableSet(propagatedSet);
        this.unchangedProviders = Collections.unmodifiableSet(unchangedSet);
        this.clearedProviders = Collections.unmodifiableSet(clearedSet);
        this.snapshotInitialSize = propagatedProviders.size() + clearedProviders.size();
        this.propagatedProvidersFastIterable = propagatedProviders.toArray(new ThreadContextProvider[0]);
        this.clearedProvidersFastIterable = clearedProviders.toArray(new ThreadContextProvider[0]);
    }

    /**
     * This helps to optimise construction of CapturedContextState
     * without exposing too many implementation details.
     * Only useful for snapshots with an empty property set.
     * 
     * @return a list of snapshots
     */
    public List<ThreadContextSnapshot> takeThreadContextSnapshots() {
        List<ThreadContextSnapshot> threadContextSnapshots = new ArrayList<>(snapshotInitialSize);
        final Map<String, String> props = Collections.emptyMap();
        for (ThreadContextProvider provider : propagatedProvidersFastIterable) {
            ThreadContextSnapshot snapshot = provider.currentContext(props);
            if (snapshot != null) {
                threadContextSnapshots.add(snapshot);
            }
        }
        for (ThreadContextProvider provider : clearedProvidersFastIterable) {
            ThreadContextSnapshot snapshot = provider.clearedContext(props);
            if (snapshot != null) {
                threadContextSnapshots.add(snapshot);
            }
        }
        return threadContextSnapshots;
    }
}

package io.smallrye.context.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.FastThreadContextProvider;
import io.smallrye.context.SmallRyeThreadContext;

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
    private boolean fast;

    public ThreadContextProviderPlan(Set<ThreadContextProvider> propagatedSet, Set<ThreadContextProvider> unchangedSet,
            Set<ThreadContextProvider> clearedSet) {
        this.propagatedProviders = Collections.unmodifiableSet(propagatedSet);
        this.unchangedProviders = Collections.unmodifiableSet(unchangedSet);
        this.clearedProviders = Collections.unmodifiableSet(clearedSet);
        this.snapshotInitialSize = propagatedProviders.size() + clearedProviders.size();
        this.propagatedProvidersFastIterable = propagatedProviders.toArray(new ThreadContextProvider[0]);
        this.clearedProvidersFastIterable = clearedProviders.toArray(new ThreadContextProvider[0]);
        boolean fast = true;
        for (ThreadContextProvider provider : propagatedProvidersFastIterable) {
            if (provider instanceof FastThreadContextProvider == false) {
                fast = false;
                break;
            }
        }
        if (fast) {
            for (ThreadContextProvider provider : clearedProvidersFastIterable) {
                if (provider instanceof FastThreadContextProvider == false) {
                    fast = false;
                    break;
                }
            }
        }
        this.fast = fast;
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

    public Object[] takeThreadContextSnapshotsFast(SmallRyeThreadContext threadContext,
            ThreadLocal<SmallRyeThreadContext> tcTl) {
        if (!fast)
            throw new IllegalStateException("This ThreadContext includes non-fast providers: " + this.clearedProviders + " and "
                    + this.propagatedProviders);
        // layout is [TL, capturedValue]*
        Object[] threadContextSnapshots = new Object[(snapshotInitialSize + 1) * 2];
        final Map<String, String> props = Collections.emptyMap();
        int i = 0;
        for (ThreadContextProvider provider : propagatedProvidersFastIterable) {
            ThreadLocal<?> tl = ((FastThreadContextProvider) provider).threadLocal(props);
            threadContextSnapshots[i++] = tl;
            threadContextSnapshots[i++] = tl.get();
        }
        for (ThreadContextProvider provider : clearedProvidersFastIterable) {
            ThreadLocal<?> tl = ((FastThreadContextProvider) provider).threadLocal(props);
            threadContextSnapshots[i++] = tl;
            threadContextSnapshots[i++] = ((FastThreadContextProvider) provider).clearedValue(props);
        }
        threadContextSnapshots[i++] = tcTl;
        threadContextSnapshots[i++] = threadContext;
        return threadContextSnapshots;
    }
}

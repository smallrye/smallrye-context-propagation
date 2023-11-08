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
import io.smallrye.context.storage.spi.ThreadScope;

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
    private final boolean fast;

    public ThreadContextProviderPlan(Set<ThreadContextProvider> propagatedSet, Set<ThreadContextProvider> unchangedSet,
            Set<ThreadContextProvider> clearedSet, boolean enableFastThreadContextProviders) {
        this.propagatedProviders = Collections.unmodifiableSet(propagatedSet);
        this.unchangedProviders = Collections.unmodifiableSet(unchangedSet);
        this.clearedProviders = Collections.unmodifiableSet(clearedSet);
        this.snapshotInitialSize = propagatedProviders.size() + clearedProviders.size();
        this.propagatedProvidersFastIterable = propagatedProviders.toArray(new ThreadContextProvider[0]);
        this.clearedProvidersFastIterable = clearedProviders.toArray(new ThreadContextProvider[0]);
        // defaults to true
        boolean fast = enableFastThreadContextProviders;
        // allow settings to disable fast path
        if (fast) {
            // true means check that all providers support it
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

    /**
     * @return true if every ThreadContextProvider of this plan implements @{link FastThreadContextProvider}
     */
    public boolean isFast() {
        return fast;
    }

    /**
     * Use this if @{link {@link #isFast()} is true (it will throw otherwise) when you want to capture the current context
     * using the fast-path, and feed the captured context in the given @{link ContextHolder}, which must have a size compatible
     * with @{link {@link #size()}.
     *
     * @param threadContext The thread context settings
     * @param tcTl the current ThreadContext thread-local (for contextual settings)
     * @param contextHolder the contextual lambda in which we will capture context
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void takeThreadContextSnapshotsFast(SmallRyeThreadContext threadContext,
            ThreadScope<SmallRyeThreadContext> tcTl,
            ContextHolder contextHolder) {
        if (!fast)
            throw new IllegalStateException("This ThreadContext includes non-fast providers: " + this.clearedProviders + " and "
                    + this.propagatedProviders);
        if (snapshotInitialSize == 0)
            throw new IllegalStateException("Don't capture empty context plans");
        final Map<String, String> props = Collections.emptyMap();
        int i = 0;
        for (ThreadContextProvider provider : propagatedProvidersFastIterable) {
            ThreadScope<?> tl = ((FastThreadContextProvider) provider).threadScope(props);
            contextHolder.captureThreadScope(i++, (ThreadScope<Object>) tl, tl.get());
        }
        for (ThreadContextProvider provider : clearedProvidersFastIterable) {
            ThreadScope<?> tl = ((FastThreadContextProvider) provider).threadScope(props);
            contextHolder.captureThreadScope(i++, (ThreadScope<Object>) tl,
                    ((FastThreadContextProvider) provider).clearedValue(props));
        }
        contextHolder.captureThreadScope(i, (ThreadScope) tcTl, threadContext);
    }

    /**
     * @return true if there are no captured/cleared contexts (all unchanged). Note: we don't count
     *         the contextual ThreadContext because we never want to capture/restore it if it's the only one.
     */
    public boolean isEmpty() {
        return snapshotInitialSize == 0;
    }

    /**
     * @return the number of captured/cleared contexts (including the contextual ThreadContext)
     */
    public int size() {
        // +1 for our own ThreadLocal<ThreadContext>
        return snapshotInitialSize + 1;
    }
}

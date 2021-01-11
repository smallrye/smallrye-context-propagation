package io.smallrye.context.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.SmallRyeThreadContext;

public class CapturedContextState {

    private List<ThreadContextSnapshot> threadContextSnapshots;
    private SmallRyeThreadContext threadContext;

    public CapturedContextState(SmallRyeThreadContext threadContext, ThreadContextProviderPlan plan) {
        this.threadContext = threadContext;
        this.threadContextSnapshots = new ArrayList<>(plan.propagatedProviders.size() + plan.clearedProviders.size());
        final Map<String, String> props = Collections.emptyMap();
        for (ThreadContextProvider provider : plan.propagatedProviders) {
            ThreadContextSnapshot snapshot = provider.currentContext(props);
            if (snapshot != null) {
                threadContextSnapshots.add(snapshot);
            }
        }
        for (ThreadContextProvider provider : plan.clearedProviders) {
            ThreadContextSnapshot snapshot = provider.clearedContext(props);
            if (snapshot != null) {
                threadContextSnapshots.add(snapshot);
            }
        }
    }

    public ActiveContextState begin() {
        return new ActiveContextState(threadContext, threadContextSnapshots);
    }
}

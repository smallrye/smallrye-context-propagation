package io.smallrye.context.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.SmallRyeThreadContext;

public class CapturedContextState {

    private List<ThreadContextSnapshot> threadContextSnapshots = new LinkedList<>();
    private SmallRyeThreadContext threadContext;

    public CapturedContextState(SmallRyeThreadContext threadContext, ThreadContextProviderPlan plan,
            Map<String, String> props) {
        this.threadContext = threadContext;
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

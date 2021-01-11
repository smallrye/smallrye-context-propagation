package io.smallrye.context.impl;

import java.util.List;

import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.SmallRyeThreadContext;

public class CapturedContextState {

    private List<ThreadContextSnapshot> threadContextSnapshots;
    private SmallRyeThreadContext threadContext;

    public CapturedContextState(SmallRyeThreadContext threadContext, ThreadContextProviderPlan plan) {
        this.threadContext = threadContext;
        this.threadContextSnapshots = plan.takeThreadContextSnapshots();
    }

    public ActiveContextState begin() {
        return new ActiveContextState(threadContext, threadContextSnapshots);
    }
}

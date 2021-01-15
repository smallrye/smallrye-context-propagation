package io.smallrye.context.impl;

import java.util.List;

import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.SmallRyeThreadContext;

public class SlowCapturedContextState implements CapturedContextState {

    private List<ThreadContextSnapshot> threadContextSnapshots;
    private SmallRyeThreadContext threadContext;

    public SlowCapturedContextState(SmallRyeThreadContext threadContext) {
        this.threadContext = threadContext;
        this.threadContextSnapshots = threadContext.getPlan().takeThreadContextSnapshots();
    }

    public SlowActiveContextState begin() {
        return new SlowActiveContextState(threadContext, threadContextSnapshots);
    }
}

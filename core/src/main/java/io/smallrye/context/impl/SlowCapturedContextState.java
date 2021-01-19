package io.smallrye.context.impl;

import java.util.List;

import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.SmallRyeThreadContext;

/**
 * Holds the list of thread context snapshots that constitute a captured context. This captures the context in the constructor,
 * and restores it in @{link {@link #begin()}
 */
public class SlowCapturedContextState implements CapturedContextState {

    private List<ThreadContextSnapshot> threadContextSnapshots;
    private SmallRyeThreadContext threadContext;

    /**
     * Captures the current context according to the given ThreadContext
     * 
     * @param threadContext the thread context
     */
    public SlowCapturedContextState(SmallRyeThreadContext threadContext) {
        this.threadContext = threadContext;
        this.threadContextSnapshots = threadContext.getPlan().takeThreadContextSnapshots();
    }

    /**
     * Restores the captured context and returns an instance that can unrestore (cleanup) it.
     * 
     * @return the captured context state
     */
    public SlowActiveContextState begin() {
        return new SlowActiveContextState(threadContext, threadContextSnapshots);
    }
}

package io.smallrye.context.impl;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeThreadContext;

public class FastActiveContextState implements CleanAutoCloseable {

    private SmallRyeThreadContext threadContext;
    private Object[] movedContext;
    private Object[] capturedContext;

    public FastActiveContextState(SmallRyeThreadContext threadContext, Object[] capturedContext) {
        this.threadContext = threadContext;
        this.capturedContext = capturedContext;
        this.movedContext = threadContext.applyContext(capturedContext);
    }

    @Override
    public void close() {
        threadContext.restoreContext(capturedContext, movedContext);
    }

}

package io.smallrye.context.impl;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeThreadContext;

public class FastCapturedContextState implements CapturedContextState {

    private Object[] capturedContext;
    private SmallRyeThreadContext threadContext;

    public FastCapturedContextState(SmallRyeThreadContext threadContext, ThreadLocal<SmallRyeThreadContext> tcTl) {
        this.threadContext = threadContext;
        this.capturedContext = threadContext.getPlan().takeThreadContextSnapshotsFast(threadContext, tcTl);
    }

    @Override
    public CleanAutoCloseable begin() {
        return new FastActiveContextState(threadContext, capturedContext);
    }

}

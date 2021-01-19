package io.smallrye.context.impl.wrappers;

import java.util.concurrent.Callable;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.impl.CapturedContextState;
import io.smallrye.context.impl.Contextualized;

public final class SlowContextualCallable<R> implements Callable<R>, Contextualized {
    private final CapturedContextState state;
    private final Callable<R> callable;

    public SlowContextualCallable(CapturedContextState state, Callable<R> callable) {
        this.state = state;
        this.callable = callable;
    }

    @Override
    public R call() throws Exception {
        try (CleanAutoCloseable activeState = state.begin()) {
            return callable.call();
        }
    }
}
package io.smallrye.context.impl.wrappers;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.impl.CapturedContextState;
import io.smallrye.context.impl.Contextualized;

public final class SlowContextualRunnable implements Runnable, Contextualized {
    private final Runnable runnable;
    private final CapturedContextState state;

    public SlowContextualRunnable(CapturedContextState state, Runnable runnable) {
        this.runnable = runnable;
        this.state = state;
    }

    @Override
    public void run() {
        try (CleanAutoCloseable<Void> activeState = state.begin(() -> {
            runnable.run();
            return null;
        })) {
            activeState.callNoChecked();
        }
    }
}

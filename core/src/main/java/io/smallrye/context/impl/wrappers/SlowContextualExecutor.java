package io.smallrye.context.impl.wrappers;

import java.util.concurrent.Executor;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.impl.CapturedContextState;

public class SlowContextualExecutor implements Executor {

    private CapturedContextState state;

    public SlowContextualExecutor(CapturedContextState state) {
        this.state = state;
    }

    @Override
    public void execute(Runnable command) {
        try (CleanAutoCloseable foo = state.begin()) {
            command.run();
        }
    }

}

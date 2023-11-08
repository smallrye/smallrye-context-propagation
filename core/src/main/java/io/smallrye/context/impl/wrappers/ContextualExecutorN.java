package io.smallrye.context.impl.wrappers;

import io.smallrye.context.storage.spi.ThreadScope;

public class ContextualExecutorN implements ContextualExecutor {
    private ThreadScope<Object>[] tl;
    private Object[] state;

    public ContextualExecutorN(int n) {
        this.tl = new ThreadScope[n];
        this.state = new Object[n];
    }

    @Override
    public void execute(Runnable command) {
        Object[] moved = new Object[tl.length];
        for (int i = 0; i < tl.length; i++) {
            moved[i] = tl[i].get();
            tl[i].set(state[i]);
        }
        try {
            command.run();
        } finally {
            for (int i = 0; i < tl.length; i++) {
                tl[i].set(moved[i]);
            }
        }
    }

    @Override
    public void captureThreadScope(int index, ThreadScope<Object> ThreadScope, Object value) {
        if (index < 0 || index >= state.length)
            throw new IllegalArgumentException("Illegal index " + index);
        tl[index] = ThreadScope;
        state[index] = value;
    }

}

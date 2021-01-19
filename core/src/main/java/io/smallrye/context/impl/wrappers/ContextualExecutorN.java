package io.smallrye.context.impl.wrappers;

public class ContextualExecutorN implements ContextualExecutor {
    private ThreadLocal<Object>[] tl;
    private Object[] state;

    public ContextualExecutorN(int n) {
        this.tl = new ThreadLocal[n];
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
    public void captureThreadLocal(int index, ThreadLocal<Object> threadLocal, Object value) {
        if (index < 0 || index >= state.length)
            throw new IllegalArgumentException("Illegal index " + index);
        tl[index] = threadLocal;
        state[index] = value;
    }

}

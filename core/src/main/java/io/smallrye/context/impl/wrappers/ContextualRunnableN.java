package io.smallrye.context.impl.wrappers;

public final class ContextualRunnableN implements ContextualRunnable {
    private ThreadLocal<Object>[] tl;
    private Object[] state;

    private final Runnable runnable;

    public ContextualRunnableN(Runnable runnable, int n) {
        this.runnable = runnable;
        this.tl = new ThreadLocal[n];
        this.state = new Object[n];
    }

    @Override
    public void run() {
        Object[] moved = new Object[tl.length];
        for (int i = 0; i < tl.length; i++) {
            moved[i] = tl[i].get();
            tl[i].set(state[i]);
        }
        try {
            runnable.run();
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

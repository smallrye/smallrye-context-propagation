package io.smallrye.context.impl.wrappers;

public final class ContextualRunnable1 implements ContextualRunnable {
    private ThreadLocal<Object> tl0;
    private Object state0;

    private final Runnable runnable;

    public ContextualRunnable1(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        Object moved0 = tl0.get();
        tl0.set(state0);
        try {
            runnable.run();
        } finally {
            tl0.set(moved0);
        }
    }

    @Override
    public void captureThreadLocal(int index, ThreadLocal<Object> threadLocal, Object value) {
        switch (index) {
            case 0:
                tl0 = threadLocal;
                state0 = value;
                break;
            default:
                throw new IllegalArgumentException("Illegal index " + index);
        }
    }
}

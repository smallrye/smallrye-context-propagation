package io.smallrye.context.impl.wrappers;

public class ContextualExecutor1 implements ContextualExecutor {
    private ThreadLocal<Object> tl0;
    private Object state0;

    @Override
    public void execute(Runnable command) {
        Object moved0 = tl0.get();
        tl0.set(state0);
        try {
            command.run();
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

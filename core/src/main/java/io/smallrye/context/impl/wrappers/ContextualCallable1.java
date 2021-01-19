package io.smallrye.context.impl.wrappers;

import java.util.concurrent.Callable;

public class ContextualCallable1<T> implements ContextualCallable<T> {
    private ThreadLocal<Object> tl0;
    private Object state0;

    private final Callable<T> callable;

    public ContextualCallable1(Callable<T> callable) {
        this.callable = callable;
    }

    @Override
    public T call() throws Exception {
        Object moved0 = tl0.get();
        tl0.set(state0);
        try {
            return callable.call();
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

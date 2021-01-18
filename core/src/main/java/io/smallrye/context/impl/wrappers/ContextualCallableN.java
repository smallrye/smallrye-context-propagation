package io.smallrye.context.impl.wrappers;

import java.util.concurrent.Callable;

public class ContextualCallableN<T> implements ContextualCallable<T> {
    private ThreadLocal<Object>[] tl;
    private Object[] state;

    private final Callable<T> callable;

    public ContextualCallableN(Callable<T> callable, int n) {
        this.callable = callable;
        this.tl = new ThreadLocal[n];
        this.state = new Object[n];
    }

    @Override
    public T call() throws Exception {
        Object[] moved = new Object[tl.length];
        for (int i = 0; i < tl.length; i++) {
            moved[i] = tl[i].get();
            tl[i].set(state[i]);
        }
        try {
            return callable.call();
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

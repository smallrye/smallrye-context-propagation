package io.smallrye.context.impl.wrappers;

import java.util.function.Function;

public class ContextualFunctionN<T, R> implements ContextualFunction<T, R> {
    private ThreadLocal<Object>[] tl;
    private Object[] state;

    private final Function<T, R> function;

    public ContextualFunctionN(Function<T, R> function, int n) {
        this.function = function;
        this.tl = new ThreadLocal[n];
        this.state = new Object[n];
    }

    @Override
    public R apply(T t) {
        Object[] moved = new Object[tl.length];
        for (int i = 0; i < tl.length; i++) {
            moved[i] = tl[i].get();
            tl[i].set(state[i]);
        }
        try {
            return function.apply(t);
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

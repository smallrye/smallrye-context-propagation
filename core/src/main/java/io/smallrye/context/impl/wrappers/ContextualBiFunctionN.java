package io.smallrye.context.impl.wrappers;

import java.util.function.BiFunction;

public class ContextualBiFunctionN<T, U, R> implements ContextualBiFunction<T, U, R> {
    private ThreadLocal<Object>[] tl;
    private Object[] state;

    private final BiFunction<T, U, R> biFunction;

    public ContextualBiFunctionN(BiFunction<T, U, R> biFunction, int n) {
        this.biFunction = biFunction;
        this.tl = new ThreadLocal[n];
        this.state = new Object[n];
    }

    @Override
    public R apply(T t, U u) {
        Object[] moved = new Object[tl.length];
        for (int i = 0; i < tl.length; i++) {
            moved[i] = tl[i].get();
            tl[i].set(state[i]);
        }
        try {
            return biFunction.apply(t, u);
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

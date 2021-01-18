package io.smallrye.context.impl.wrappers;

import java.util.function.BiFunction;

public class ContextualBiFunction1<T, U, R> implements ContextualBiFunction<T, U, R> {
    private ThreadLocal<Object> tl0;
    private Object state0;

    private final BiFunction<T, U, R> biFunction;

    public ContextualBiFunction1(BiFunction<T, U, R> biFunction) {
        this.biFunction = biFunction;
    }

    @Override
    public R apply(T t, U u) {
        Object moved0 = tl0.get();
        tl0.set(state0);
        try {
            return biFunction.apply(t, u);
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

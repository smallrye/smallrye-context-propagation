package io.smallrye.context.impl.wrappers;

import java.util.function.BiFunction;

import io.smallrye.context.storage.spi.ThreadScope;

public class ContextualBiFunction2<T, U, R> implements ContextualBiFunction<T, U, R> {
    private ThreadScope<Object> tl0;
    private Object state0;
    private ThreadScope<Object> tl1;
    private Object state1;

    private final BiFunction<T, U, R> biFunction;

    public ContextualBiFunction2(BiFunction<T, U, R> biFunction) {
        this.biFunction = biFunction;
    }

    @Override
    public R apply(T t, U u) {
        Object moved0 = tl0.get();
        tl0.set(state0);
        Object moved1 = tl1.get();
        tl1.set(state1);
        try {
            return biFunction.apply(t, u);
        } finally {
            tl0.set(moved0);
            tl1.set(moved1);
        }
    }

    @Override
    public void captureThreadScope(int index, ThreadScope<Object> ThreadScope, Object value) {
        switch (index) {
            case 0:
                tl0 = ThreadScope;
                state0 = value;
                break;
            case 1:
                tl1 = ThreadScope;
                state1 = value;
                break;
            default:
                throw new IllegalArgumentException("Illegal index " + index);
        }
    }

}

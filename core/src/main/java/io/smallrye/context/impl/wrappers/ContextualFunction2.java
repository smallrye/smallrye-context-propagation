package io.smallrye.context.impl.wrappers;

import java.util.function.Function;

import io.smallrye.context.storage.spi.ThreadScope;

public class ContextualFunction2<T, R> implements ContextualFunction<T, R> {
    private ThreadScope<Object> tl0;
    private Object state0;
    private ThreadScope<Object> tl1;
    private Object state1;

    private final Function<T, R> function;

    public ContextualFunction2(Function<T, R> function) {
        this.function = function;
    }

    @Override
    public R apply(T t) {
        Object moved0 = tl0.get();
        tl0.set(state0);
        Object moved1 = tl1.get();
        tl1.set(state1);
        try {
            return function.apply(t);
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

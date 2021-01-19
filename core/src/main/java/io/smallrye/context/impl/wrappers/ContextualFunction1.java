package io.smallrye.context.impl.wrappers;

import java.util.function.Function;

public class ContextualFunction1<T, R> implements ContextualFunction<T, R> {
    private ThreadLocal<Object> tl0;
    private Object state0;

    private final Function<T, R> function;

    public ContextualFunction1(Function<T, R> function) {
        this.function = function;
    }

    @Override
    public R apply(T t) {
        Object moved0 = tl0.get();
        tl0.set(state0);
        try {
            return function.apply(t);
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

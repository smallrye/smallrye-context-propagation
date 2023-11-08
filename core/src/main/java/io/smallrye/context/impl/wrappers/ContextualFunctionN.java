package io.smallrye.context.impl.wrappers;

import java.util.function.Function;

import io.smallrye.context.storage.spi.ThreadScope;

public class ContextualFunctionN<T, R> implements ContextualFunction<T, R> {
    private ThreadScope<Object>[] tl;
    private Object[] state;

    private final Function<T, R> function;

    public ContextualFunctionN(Function<T, R> function, int n) {
        this.function = function;
        this.tl = new ThreadScope[n];
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
    public void captureThreadScope(int index, ThreadScope<Object> ThreadScope, Object value) {
        if (index < 0 || index >= state.length)
            throw new IllegalArgumentException("Illegal index " + index);
        tl[index] = ThreadScope;
        state[index] = value;
    }

}

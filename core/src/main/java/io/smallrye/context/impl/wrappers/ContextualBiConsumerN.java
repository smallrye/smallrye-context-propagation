package io.smallrye.context.impl.wrappers;

import java.util.function.BiConsumer;

public class ContextualBiConsumerN<T, U> implements ContextualBiConsumer<T, U> {
    private ThreadLocal<Object>[] tl;
    private Object[] state;

    private final BiConsumer<T, U> biConsumer;

    public ContextualBiConsumerN(BiConsumer<T, U> biConsumer, int n) {
        this.biConsumer = biConsumer;
        this.tl = new ThreadLocal[n];
        this.state = new Object[n];
    }

    @Override
    public void accept(T t, U u) {
        Object[] moved = new Object[tl.length];
        for (int i = 0; i < tl.length; i++) {
            moved[i] = tl[i].get();
            tl[i].set(state[i]);
        }
        try {
            biConsumer.accept(t, u);
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

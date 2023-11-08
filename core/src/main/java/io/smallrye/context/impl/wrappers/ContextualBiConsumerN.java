package io.smallrye.context.impl.wrappers;

import java.util.function.BiConsumer;

import io.smallrye.context.storage.spi.ThreadScope;

public class ContextualBiConsumerN<T, U> implements ContextualBiConsumer<T, U> {
    private ThreadScope<Object>[] tl;
    private Object[] state;

    private final BiConsumer<T, U> biConsumer;

    public ContextualBiConsumerN(BiConsumer<T, U> biConsumer, int n) {
        this.biConsumer = biConsumer;
        this.tl = new ThreadScope[n];
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
    public void captureThreadScope(int index, ThreadScope<Object> ThreadScope, Object value) {
        if (index < 0 || index >= state.length)
            throw new IllegalArgumentException("Illegal index " + index);
        tl[index] = ThreadScope;
        state[index] = value;
    }

}

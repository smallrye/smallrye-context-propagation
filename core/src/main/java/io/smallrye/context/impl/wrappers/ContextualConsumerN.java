package io.smallrye.context.impl.wrappers;

import java.util.function.Consumer;

import io.smallrye.context.storage.spi.ThreadScope;

public class ContextualConsumerN<T> implements ContextualConsumer<T> {
    private ThreadScope<Object>[] tl;
    private Object[] state;

    private final Consumer<T> consumer;

    public ContextualConsumerN(Consumer<T> consumer, int n) {
        this.consumer = consumer;
        this.tl = new ThreadScope[n];
        this.state = new Object[n];
    }

    @Override
    public void accept(T t) {
        Object[] moved = new Object[tl.length];
        for (int i = 0; i < tl.length; i++) {
            moved[i] = tl[i].get();
            tl[i].set(state[i]);
        }
        try {
            consumer.accept(t);
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

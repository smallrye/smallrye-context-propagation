package io.smallrye.context.impl.wrappers;

import java.util.function.BiConsumer;

import io.smallrye.context.storage.spi.ThreadScope;

public class ContextualBiConsumer1<T, U> implements ContextualBiConsumer<T, U> {
    private ThreadScope<Object> tl0;
    private Object state0;

    private final BiConsumer<T, U> biConsumer;

    public ContextualBiConsumer1(BiConsumer<T, U> biConsumer) {
        this.biConsumer = biConsumer;
    }

    @Override
    public void accept(T t, U u) {
        Object moved0 = tl0.get();
        tl0.set(state0);
        try {
            biConsumer.accept(t, u);
        } finally {
            tl0.set(moved0);
        }
    }

    @Override
    public void captureThreadScope(int index, ThreadScope<Object> ThreadScope, Object value) {
        switch (index) {
            case 0:
                tl0 = ThreadScope;
                state0 = value;
                break;
            default:
                throw new IllegalArgumentException("Illegal index " + index);
        }
    }

}

package io.smallrye.context.impl.wrappers;

import java.util.function.BiConsumer;

public class ContextualBiConsumer1<T, U> implements ContextualBiConsumer<T, U> {
    private ThreadLocal<Object> tl0;
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

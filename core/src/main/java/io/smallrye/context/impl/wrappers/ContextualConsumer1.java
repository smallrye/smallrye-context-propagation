package io.smallrye.context.impl.wrappers;

import java.util.function.Consumer;

public class ContextualConsumer1<T> implements ContextualConsumer<T> {
    private ThreadLocal<Object> tl0;
    private Object state0;

    private final Consumer<T> consumer;

    public ContextualConsumer1(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(T t) {
        Object moved0 = tl0.get();
        tl0.set(state0);
        try {
            consumer.accept(t);
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

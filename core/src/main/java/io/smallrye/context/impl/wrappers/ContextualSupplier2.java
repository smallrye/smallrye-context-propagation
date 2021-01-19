package io.smallrye.context.impl.wrappers;

import java.util.function.Supplier;

public final class ContextualSupplier2<R> implements ContextualSupplier<R> {
    private ThreadLocal<Object> tl0;
    private Object state0;
    private ThreadLocal<Object> tl1;
    private Object state1;

    private final Supplier<R> supplier;

    public ContextualSupplier2(Supplier<R> supplier) {
        this.supplier = supplier;
    }

    @Override
    public R get() {
        Object moved0 = tl0.get();
        tl0.set(state0);
        Object moved1 = tl1.get();
        tl1.set(state1);
        try {
            return supplier.get();
        } finally {
            tl0.set(moved0);
            tl1.set(moved1);
        }
    }

    @Override
    public void captureThreadLocal(int index, ThreadLocal<Object> threadLocal, Object value) {
        switch (index) {
            case 0:
                tl0 = threadLocal;
                state0 = value;
                break;
            case 1:
                tl1 = threadLocal;
                state1 = value;
                break;
            default:
                throw new IllegalArgumentException("Illegal index " + index);
        }
    }
}

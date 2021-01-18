package io.smallrye.context.impl.wrappers;

import java.util.function.Supplier;

public final class ContextualSupplier1<R> implements ContextualSupplier<R> {
    private ThreadLocal<Object> tl0;
    private Object state0;

    private final Supplier<R> supplier;

    public ContextualSupplier1(Supplier<R> supplier) {
        this.supplier = supplier;
    }

    @Override
    public R get() {
        Object moved0 = tl0.get();
        tl0.set(state0);
        try {
            return supplier.get();
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

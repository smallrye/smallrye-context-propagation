package io.smallrye.context.impl.wrappers;

import java.util.function.Supplier;

public final class ContextualSupplierN<R> implements ContextualSupplier<R> {
    private ThreadLocal<Object>[] tl;
    private Object[] state;

    private final Supplier<R> supplier;

    public ContextualSupplierN(Supplier<R> supplier, int n) {
        this.supplier = supplier;
        this.tl = new ThreadLocal[n];
        this.state = new Object[n];
    }

    @Override
    public R get() {
        Object[] moved = new Object[tl.length];
        for (int i = 0; i < tl.length; i++) {
            moved[i] = tl[i].get();
            tl[i].set(state[i]);
        }
        try {
            return supplier.get();
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

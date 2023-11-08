package io.smallrye.context.impl.wrappers;

import java.util.function.Supplier;

import io.smallrye.context.storage.spi.ThreadScope;

public final class ContextualSupplier2<R> implements ContextualSupplier<R> {
    private ThreadScope<Object> tl0;
    private Object state0;
    private ThreadScope<Object> tl1;
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
    public void captureThreadScope(int index, ThreadScope<Object> threadScope, Object value) {
        switch (index) {
            case 0:
                tl0 = ThreadScope;
                state0 = value;
                break;
            case 1:
                tl1 = ThreadScope;
                state1 = value;
                break;
            default:
                throw new IllegalArgumentException("Illegal index " + index);
        }
    }
}

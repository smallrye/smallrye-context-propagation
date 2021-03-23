package io.smallrye.context.impl.wrappers;

import java.util.function.Supplier;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.impl.CapturedContextState;
import io.smallrye.context.impl.Contextualized;

public final class SlowContextualSupplier<R> implements Supplier<R>, Contextualized {
    private final CapturedContextState state;
    private final Supplier<R> supplier;

    public SlowContextualSupplier(CapturedContextState state, Supplier<R> supplier) {
        this.state = state;
        this.supplier = supplier;
    }

    @Override
    public R get() {
        try (CleanAutoCloseable<R> activeState = state.begin(supplier::get)) {
            return activeState.callNoChecked();
        }
    }
}

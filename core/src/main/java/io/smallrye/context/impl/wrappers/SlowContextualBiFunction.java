package io.smallrye.context.impl.wrappers;

import java.util.function.BiFunction;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.impl.CapturedContextState;
import io.smallrye.context.impl.Contextualized;

public final class SlowContextualBiFunction<T, U, R> implements BiFunction<T, U, R>, Contextualized {
    private final CapturedContextState state;
    private final BiFunction<T, U, R> function;

    public SlowContextualBiFunction(CapturedContextState state, BiFunction<T, U, R> function) {
        this.state = state;
        this.function = function;
    }

    @Override
    public R apply(T t, U u) {
        try (CleanAutoCloseable<R> activeState = state.begin(() -> function.apply(t, u))) {
            return activeState.callNoChecked();
        }
    }
}
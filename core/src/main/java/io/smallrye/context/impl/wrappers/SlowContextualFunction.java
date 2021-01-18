package io.smallrye.context.impl.wrappers;

import java.util.function.Function;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.impl.CapturedContextState;
import io.smallrye.context.impl.Contextualized;

public final class SlowContextualFunction<T, R> implements Function<T, R>, Contextualized {
    private final CapturedContextState state;
    private final Function<T, R> function;

    public SlowContextualFunction(CapturedContextState state, Function<T, R> function) {
        this.state = state;
        this.function = function;
    }

    @Override
    public R apply(T t) {
        try (CleanAutoCloseable activeState = state.begin()) {
            return function.apply(t);
        }
    }
}
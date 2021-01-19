package io.smallrye.context.impl.wrappers;

import java.util.function.BiConsumer;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.impl.CapturedContextState;
import io.smallrye.context.impl.Contextualized;

public final class SlowContextualBiConsumer<T, U> implements BiConsumer<T, U>, Contextualized {
    private final BiConsumer<T, U> consumer;
    private final CapturedContextState state;

    public SlowContextualBiConsumer(CapturedContextState state, BiConsumer<T, U> consumer) {
        this.consumer = consumer;
        this.state = state;
    }

    @Override
    public void accept(T t, U u) {
        try (CleanAutoCloseable activeState = state.begin()) {
            consumer.accept(t, u);
        }
    }
}
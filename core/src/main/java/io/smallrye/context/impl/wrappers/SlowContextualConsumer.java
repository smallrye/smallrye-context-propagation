package io.smallrye.context.impl.wrappers;

import java.util.function.Consumer;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.impl.CapturedContextState;
import io.smallrye.context.impl.Contextualized;

public final class SlowContextualConsumer<T> implements Consumer<T>, Contextualized {
    private final CapturedContextState state;
    private final Consumer<T> consumer;

    public SlowContextualConsumer(CapturedContextState state, Consumer<T> consumer) {
        this.state = state;
        this.consumer = consumer;
    }

    @Override
    public void accept(T t) {
        try (CleanAutoCloseable<Void> activeState = state.begin(() -> {
            consumer.accept(t);
            return null;
        })) {
            activeState.callNoChecked();
        }
    }
}
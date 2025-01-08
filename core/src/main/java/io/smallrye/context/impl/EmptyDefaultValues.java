package io.smallrye.context.impl;

import io.smallrye.context.api.ManagedExecutorConfig;

final class EmptyDefaultValues implements DefaultValues {

    static final EmptyDefaultValues INSTANCE = new EmptyDefaultValues();

    private static final String[] EMPTY_ARRAY = new String[0];

    private EmptyDefaultValues() {
    }

    @Override
    public String[] getExecutorPropagated() {
        return EMPTY_ARRAY;
    }

    @Override
    public String[] getExecutorCleared() {
        return EMPTY_ARRAY;
    }

    @Override
    public int getExecutorAsync() {
        return ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxAsync();
    }

    @Override
    public int getExecutorQueue() {
        return ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxQueued();
    }

    @Override
    public String[] getThreadPropagated() {
        return EMPTY_ARRAY;
    }

    @Override
    public String[] getThreadCleared() {
        return EMPTY_ARRAY;
    }

    @Override
    public String[] getThreadUnchanged() {
        return EMPTY_ARRAY;
    }
}

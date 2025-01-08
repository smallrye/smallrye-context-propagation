package io.smallrye.context.impl;

public interface DefaultValues {

    String[] getExecutorPropagated();

    String[] getExecutorCleared();

    int getExecutorAsync();

    int getExecutorQueue();

    String[] getThreadPropagated();

    String[] getThreadCleared();

    String[] getThreadUnchanged();

    static DefaultValues empty() {
        return EmptyDefaultValues.INSTANCE;
    }
}

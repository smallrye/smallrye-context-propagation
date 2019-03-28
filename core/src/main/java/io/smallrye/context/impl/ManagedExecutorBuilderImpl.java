package io.smallrye.context.impl;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ManagedExecutor.Builder;

import io.smallrye.context.SmallRyeContextManager;

public class ManagedExecutorBuilderImpl implements ManagedExecutor.Builder {

    private SmallRyeContextManager manager;
    private int maxAsync;
    private int maxQueued;
    private String[] propagated;
    private String[] cleared;
    private String injectionPointName = null;

    public ManagedExecutorBuilderImpl(SmallRyeContextManager manager) {
        this.manager = manager;
        // initiate with default values
        this.propagated = DefaultValues.INSTANCE.getExecutorPropagated();
        this.cleared = DefaultValues.INSTANCE.getExecutorCleared();
        this.maxAsync = DefaultValues.INSTANCE.getExecutorAsync();
        this.maxQueued = DefaultValues.INSTANCE.getExecutorQueue();
    }

    @Override
    public ManagedExecutor build() {
        return new ManagedExecutorImpl(maxAsync, maxQueued,
                new ThreadContextImpl(manager, propagated, SmallRyeContextManager.NO_STRING, cleared), injectionPointName);
    }

    @Override
    public ManagedExecutor.Builder propagated(String... types) {
        this.propagated = types;
        return this;
    }

    @Override
    public ManagedExecutor.Builder maxAsync(int max) {
        if (max == 0 || max < -1) {
            throw new IllegalArgumentException("ManagedExecutor parameter maxAsync cannot be 0 or lower then -1.");
        }
        this.maxAsync = max;
        return this;
    }

    @Override
    public ManagedExecutor.Builder maxQueued(int max) {
        if (max == 0 || max < -1) {
            throw new IllegalArgumentException("ManagedExecutor parameter maxQueued cannot be 0 or lower than -1.");
        }
        this.maxQueued = max;
        return this;
    }

    @Override
    public Builder cleared(String... types) {
        this.cleared = types;
        return this;
    }

    public ManagedExecutor.Builder injectionPointName(String name) {
        this.injectionPointName = name;
        return this;
    }

}

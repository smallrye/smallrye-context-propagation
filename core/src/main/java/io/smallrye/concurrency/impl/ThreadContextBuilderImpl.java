package io.smallrye.concurrency.impl;

import org.eclipse.microprofile.context.ThreadContext;

import io.smallrye.concurrency.SmallRyeConcurrencyManager;

public class ThreadContextBuilderImpl implements ThreadContext.Builder {

    private String[] propagated;
    private String[] unchanged;
    private String[] cleared;
    private SmallRyeConcurrencyManager manager;
    private String injectionPointName = null;

    public ThreadContextBuilderImpl(SmallRyeConcurrencyManager manager) {
        this.manager = manager;
        this.propagated = DefaultValues.INSTANCE.getThreadPropagated();
        this.unchanged = DefaultValues.INSTANCE.getThreadUnchanged();
        this.cleared = DefaultValues.INSTANCE.getThreadCleared();
    }

    @Override
    public ThreadContext build() {
        return new ThreadContextImpl(manager, propagated, unchanged, cleared, injectionPointName);
    }

    @Override
    public ThreadContext.Builder propagated(String... types) {
        propagated = types;
        return this;
    }

    @Override
    public ThreadContext.Builder unchanged(String... types) {
        unchanged = types;
        return this;
    }

    @Override
    public ThreadContext.Builder cleared(String... types) {
        cleared = types;
        return this;
    }

    public ThreadContext.Builder injectionPointName(String name) {
        this.injectionPointName = name;
        return this;
    }

}

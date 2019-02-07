package io.smallrye.concurrency.impl;

import org.eclipse.microprofile.concurrent.ThreadContext;

import io.smallrye.concurrency.SmallRyeConcurrencyManager;

public class ThreadContextBuilderImpl implements ThreadContext.Builder {

    private String[] propagated;
    private String[] unchanged;
    private String[] cleared;
    private SmallRyeConcurrencyManager manager;

    public ThreadContextBuilderImpl(SmallRyeConcurrencyManager manager) {
        this.manager = manager;
        this.propagated = SmallRyeConcurrencyManager.ALL_REMAINING_ARRAY;
        this.unchanged = SmallRyeConcurrencyManager.NO_STRING;
        this.cleared = SmallRyeConcurrencyManager.TRANSACTION_ARRAY;
    }

    @Override
    public ThreadContext build() {
        return new ThreadContextImpl(manager, propagated, unchanged, cleared);
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

}

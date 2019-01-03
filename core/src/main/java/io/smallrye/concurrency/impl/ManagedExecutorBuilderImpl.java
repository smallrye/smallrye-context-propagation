package io.smallrye.concurrency.impl;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutor.Builder;

import io.smallrye.concurrency.SmallRyeConcurrencyManager;

public class ManagedExecutorBuilderImpl implements ManagedExecutor.Builder {

	private SmallRyeConcurrencyManager manager;
	private int maxAsync = -1;
	private int maxQueued = -1;
	private String[] propagated;
	private String[] cleared;

	public ManagedExecutorBuilderImpl(SmallRyeConcurrencyManager manager) {
		this.manager = manager;
		propagated = SmallRyeConcurrencyManager.ALL_REMAINING_ARRAY;
		cleared = SmallRyeConcurrencyManager.TRANSACTION_ARRAY;
	}

	@Override
	public ManagedExecutor build() {
		return new ManagedExecutorImpl(maxAsync, maxQueued, new ThreadContextImpl(manager, propagated, SmallRyeConcurrencyManager.NO_STRING, cleared));
	}

	@Override
	public ManagedExecutor.Builder propagated(String... types) {
		this.propagated = types;
		return this;
	}

	@Override
	public ManagedExecutor.Builder maxAsync(int max) {
	    if(max < -1)
	        throw new IllegalArgumentException("Invalid value for maxAsync: "+max+" (must be -1 or >= 0)");
		this.maxAsync = max;
		return this;
	}

	@Override
	public ManagedExecutor.Builder maxQueued(int max) {
        if(max <= 0 && max != -1)
            throw new IllegalArgumentException("Invalid value for maxQueued: "+max+" (must be -1 or > 0)");
		this.maxQueued = max;
		return this;
	}

	@Override
	public Builder cleared(String... types) {
		this.cleared = types;
		return this;
	}

}

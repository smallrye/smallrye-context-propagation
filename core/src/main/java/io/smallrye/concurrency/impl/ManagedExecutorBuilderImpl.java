package io.smallrye.concurrency.impl;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutor.Builder;
import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;

import io.smallrye.concurrency.SmallRyeConcurrencyManager;

public class ManagedExecutorBuilderImpl implements ManagedExecutor.Builder {

	private SmallRyeConcurrencyManager manager;
	private int maxAsync;
	private int maxQueued;
	private String[] propagated;
	private String[] cleared;

	public ManagedExecutorBuilderImpl(SmallRyeConcurrencyManager manager) {
		this.manager = manager;
		// initiate with default values
		this.propagated = SmallRyeConcurrencyManager.ALL_REMAINING_ARRAY;
		this.cleared = SmallRyeConcurrencyManager.TRANSACTION_ARRAY;
		this.maxAsync = ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxAsync();
		this.maxQueued = ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxQueued();
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

}

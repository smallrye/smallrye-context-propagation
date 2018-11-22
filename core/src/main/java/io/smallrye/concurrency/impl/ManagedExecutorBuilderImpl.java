package io.smallrye.concurrency.impl;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutor.Builder;

import io.smallrye.concurrency.SmallRyeConcurrencyManager;

public class ManagedExecutorBuilderImpl implements ManagedExecutor.Builder {

	private SmallRyeConcurrencyManager manager;
	private int maxAsync;
	private int maxQueued;
	private String[] propagated;
	private String[] cleared;

	public ManagedExecutorBuilderImpl(SmallRyeConcurrencyManager manager) {
		this.manager = manager;
		propagated = manager.getAllProviderTypes();
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
		this.maxAsync = max;
		return this;
	}

	@Override
	public ManagedExecutor.Builder maxQueued(int max) {
		this.maxQueued = max;
		return this;
	}

	@Override
	public Builder cleared(String... types) {
		this.cleared = types;
		return this;
	}

}

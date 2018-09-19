package io.smallrye.concurrency.impl;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.ThreadContextBuilder;

import io.smallrye.concurrency.SmallRyeConcurrencyManager;

public class ThreadContextBuilderImpl implements ThreadContextBuilder {

	private String[] propagated;
	private String[] unchanged;
	private SmallRyeConcurrencyManager manager;

	public ThreadContextBuilderImpl(SmallRyeConcurrencyManager manager) {
		this.manager = manager;
		this.propagated = manager.getAllProviderTypes();
		this.unchanged = SmallRyeConcurrencyManager.NO_STRING;
	}

	@Override
	public ThreadContext build() {
		return new ThreadContextImpl(manager, propagated, unchanged);
	}

	@Override
	public ThreadContextBuilder propagated(String... types) {
		propagated = types;
		return this;
	}

	@Override
	public ThreadContextBuilder unchanged(String... types) {
		unchanged = types;
		return this;
	}

}

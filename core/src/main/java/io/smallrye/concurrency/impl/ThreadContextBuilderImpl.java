package io.smallrye.concurrency.impl;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.ThreadContextBuilder;

import io.smallrye.concurrency.SmallRyeConcurrencyManager;

public class ThreadContextBuilderImpl implements ThreadContextBuilder {

	private String[] propagated;
	private String[] unchanged;
	private SmallRyeConcurrencyManager context;

	public ThreadContextBuilderImpl(SmallRyeConcurrencyManager context) {
		this.context = context;
	}

	@Override
	public ThreadContext build() {
		return new ThreadContextImpl(context, propagated, unchanged);
	}

	@Override
	public ThreadContextBuilder propagate(String... types) {
		propagated = types;
		return this;
	}

	@Override
	public ThreadContextBuilder unchanged(String... types) {
		unchanged = types;
		return this;
	}

}

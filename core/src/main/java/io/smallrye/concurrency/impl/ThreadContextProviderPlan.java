package io.smallrye.concurrency.impl;

import java.util.List;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

public class ThreadContextProviderPlan {

	public final List<ThreadContextProvider> clearedProviders;
	public final List<ThreadContextProvider> propagatedProviders;

	public ThreadContextProviderPlan(List<ThreadContextProvider> propagatedProviders,
			List<ThreadContextProvider> clearedProviders) {
		this.propagatedProviders = propagatedProviders;
		this.clearedProviders = clearedProviders;
	}
}

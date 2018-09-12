package io.smallrye.concurrency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.ServiceLoader;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

import io.smallrye.concurrency.spi.ThreadContextPropagator;

public class SmallRyeConcurrencyManager {
	
	private ArrayList<ThreadContextProvider> providers;
	private ArrayList<ThreadContextPropagator> propagators;

	public SmallRyeConcurrencyManager() {
		providers = new ArrayList<ThreadContextProvider>();
		for (ThreadContextProvider provider : ServiceLoader.load(ThreadContextProvider.class)) {
			providers.add(provider);
		}
		propagators = new ArrayList<ThreadContextPropagator>();
		for (ThreadContextPropagator propagator : ServiceLoader.load(ThreadContextPropagator.class)) {
			propagators.add(propagator);
			propagator.setup();
		}
	}
	
	public CapturedContextState captureContext() {
		Map<String, String> props = Collections.emptyMap();
		return new CapturedContextState(this, providers, props);
	}
}

package io.smallrye.concurrency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyManager;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

import io.smallrye.concurrency.impl.ManagedExecutorBuilderImpl;
import io.smallrye.concurrency.impl.ThreadContextBuilderImpl;
import io.smallrye.concurrency.impl.ThreadContextProviderPlan;
import io.smallrye.concurrency.spi.ThreadContextPropagator;

public class SmallRyeConcurrencyManager implements ConcurrencyManager {
	
	public static final String[] NO_STRING = new String[0];
	
	private List<ThreadContextProvider> providers;
	private List<ThreadContextPropagator> propagators;
	private Map<String, ThreadContextProvider> providersByType;

	private String[] allProviderTypes;
	
	SmallRyeConcurrencyManager(List<ThreadContextProvider> providers, List<ThreadContextPropagator> propagators) {
		this.providers = new ArrayList<ThreadContextProvider>(providers);
		providersByType = new HashMap<>();
		for (ThreadContextProvider provider : providers) {
			providersByType.put(provider.getThreadContextType(), provider);
		}
		// FIXME: check for duplicate types
		// FIXME: check for cycles
		allProviderTypes = providersByType.keySet().toArray(new String[this.providers.size()]);
		this.propagators = new ArrayList<ThreadContextPropagator>(propagators);
		for (ThreadContextPropagator propagator : propagators) {
			propagator.setup(this);
		}
	}
	
	public String[] getAllProviderTypes() {
		return allProviderTypes;
	}
	
	public CapturedContextState captureContext() {
		Map<String, String> props = Collections.emptyMap();
		return new CapturedContextState(this, getProviders(), props);
	}

	public CapturedContextState captureContext(String[] propagated, String[] unchanged, String[] cleared) {
		Map<String, String> props = Collections.emptyMap();
		return new CapturedContextState(this, getProviders(propagated, unchanged, cleared), props);
	}

	// package-protected for tests
	ThreadContextProviderPlan getProviders() {
		return getProviders(allProviderTypes, NO_STRING, NO_STRING);
	}
	
	// package-protected for tests
	ThreadContextProviderPlan getProviders(String[] propagated, String[] unchanged, String[] cleared) {
		Set<String> propagatedSet = new HashSet<>();
		Collections.addAll(propagatedSet, propagated);
		
		Set<String> clearedSet = new HashSet<>();
		Collections.addAll(clearedSet, cleared);

		Set<String> unchangedSet = new HashSet<>();
		Collections.addAll(unchangedSet, unchanged);

		// check for duplicates
		if(propagatedSet.removeAll(unchangedSet) || propagatedSet.removeAll(clearedSet)
				|| clearedSet.removeAll(propagatedSet) || clearedSet.removeAll(unchangedSet)
				|| unchangedSet.removeAll(propagatedSet) || unchangedSet.removeAll(clearedSet)) {
			throw new IllegalArgumentException("Cannot use ALL_REMAINING in more than one of propagated, cleared, unchanged");
		}

		// expand ALL_REMAINING
		if(propagatedSet.contains(ThreadContext.ALL_REMAINING)) {
			propagatedSet.remove(ThreadContext.ALL_REMAINING);
			Collections.addAll(propagatedSet, allProviderTypes);
			propagatedSet.removeAll(clearedSet);
			propagatedSet.removeAll(unchangedSet);
		}

		if(clearedSet.contains(ThreadContext.ALL_REMAINING)) {
			clearedSet.remove(ThreadContext.ALL_REMAINING);
			Collections.addAll(clearedSet, allProviderTypes);
			clearedSet.removeAll(propagatedSet);
			clearedSet.removeAll(unchangedSet);
		}

		if(unchangedSet.contains(ThreadContext.ALL_REMAINING)) {
			unchangedSet.remove(ThreadContext.ALL_REMAINING);
			Collections.addAll(unchangedSet, allProviderTypes);
			unchangedSet.removeAll(propagatedSet);
			unchangedSet.removeAll(clearedSet);
		}

		return new ThreadContextProviderPlan(propagatedSet.stream().map(name -> providersByType.get(name)).collect(Collectors.toSet()), 
				unchangedSet.stream().map(name -> providersByType.get(name)).collect(Collectors.toSet()), 
				clearedSet.stream().map(name -> providersByType.get(name)).collect(Collectors.toSet()));
	}

	@Override
	public ManagedExecutor.Builder newManagedExecutorBuilder() {
		return new ManagedExecutorBuilderImpl(this);
	}

	@Override
	public ThreadContext.Builder newThreadContextBuilder() {
		return new ThreadContextBuilderImpl(this);
	}

	// For tests
	public List<ThreadContextPropagator> getPropagators() {
		return propagators;
	}
}

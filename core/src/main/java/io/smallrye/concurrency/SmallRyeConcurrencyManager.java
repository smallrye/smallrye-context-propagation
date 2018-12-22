package io.smallrye.concurrency;

import java.util.*;
import java.util.stream.Collectors;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyManager;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyManagerExtension;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

import io.smallrye.concurrency.impl.ManagedExecutorBuilderImpl;
import io.smallrye.concurrency.impl.ThreadContextBuilderImpl;
import io.smallrye.concurrency.impl.ThreadContextProviderPlan;

public class SmallRyeConcurrencyManager implements ConcurrencyManager {
	
	public static final String[] NO_STRING = new String[0];

	public static final String[] ALL_REMAINING_ARRAY = new String[] { ThreadContext.ALL_REMAINING };
	public static final String[] TRANSACTION_ARRAY = new String[] { ThreadContext.TRANSACTION };
	
	private List<ThreadContextProvider> providers;
	private List<ConcurrencyManagerExtension> extensions;
	private Map<String, ThreadContextProvider> providersByType;

	private String[] allProviderTypes;
	
	SmallRyeConcurrencyManager(List<ThreadContextProvider> providers, List<ConcurrencyManagerExtension> extensions) {
		this.providers = new ArrayList<ThreadContextProvider>(providers);
		providersByType = new HashMap<>();
		for (ThreadContextProvider provider : providers) {
			providersByType.put(provider.getThreadContextType(), provider);
		}
		// FIXME: check for duplicate types
		// FIXME: check for cycles
		allProviderTypes = providersByType.keySet().toArray(new String[this.providers.size()]);
		this.extensions = new ArrayList<ConcurrencyManagerExtension>(extensions);
		for (ConcurrencyManagerExtension extension : extensions) {
			extension.setup(this);
		}
	}
	
	public String[] getAllProviderTypes() {
		return allProviderTypes;
	}
	
	public CapturedContextState captureContext() {
		Map<String, String> props = Collections.emptyMap();
		return new CapturedContextState(this, getProviderPlan(), props);
	}

	public CapturedContextState captureContext(String[] propagated, String[] unchanged, String[] cleared) {
		Map<String, String> props = Collections.emptyMap();
		return new CapturedContextState(this, getProviderPlan(propagated, unchanged, cleared), props);
	}

	// for tests
	public ThreadContextProviderPlan getProviderPlan() {
		return getProviderPlan(allProviderTypes, NO_STRING, NO_STRING);
	}
	
	// package-protected for tests
	ThreadContextProviderPlan getProviderPlan(String[] propagated, String[] unchanged, String[] cleared) {
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

		// check for existence
		Set<ThreadContextProvider> propagatedProviders = new HashSet<>();
		for(String type : propagatedSet) {
			ThreadContextProvider provider = providersByType.get(type);
			if(provider == null)
				// TODO this may be redundant as spec requires to perform this check on Builder.build() call anyway
				throw new IllegalArgumentException("Missing propagated provider type: "+type);
			propagatedProviders.add(provider);
		}

		// ignore missing for cleared/unchanged
		Set<ThreadContextProvider> unchangedProviders = new HashSet<>();
		for(String type : unchangedSet) {
			ThreadContextProvider provider = providersByType.get(type);
			if(provider != null)
				unchangedProviders.add(provider);
		}

		Set<ThreadContextProvider> clearedProviders = new HashSet<>();
		for(String type : clearedSet) {
			ThreadContextProvider provider = providersByType.get(type);
			if(provider != null)
				clearedProviders.add(provider);
		}

		// lastly, add all unmentioned contexts to cleared list
		Set<String> unmentionedContexts = this.getAllThreadContextProviderNames();
		unmentionedContexts.removeAll(unchangedSet);
		unmentionedContexts.removeAll(propagatedSet);
		unmentionedContexts.removeAll(clearedSet);
		for (String type : unmentionedContexts) {
			ThreadContextProvider provider = providersByType.get(type);
			if (provider != null) {
				clearedProviders.add(provider);
			}
		}

		return new ThreadContextProviderPlan(propagatedProviders, unchangedProviders, clearedProviders);
	}

	@Override
	public ManagedExecutor.Builder newManagedExecutorBuilder() {
		return new ManagedExecutorBuilderImpl(this);
	}

	@Override
	public ThreadContext.Builder newThreadContextBuilder() {
		return new ThreadContextBuilderImpl(this);
	}

	public Set<String> getAllThreadContextProviderNames() {
		Set<String> result = providers.stream().map(tcp -> tcp.getThreadContextType()).collect(Collectors.toSet());
		// FIXME following contexts are to be default part of implementation and should always be known
		// remove once implemented
		result.add(ThreadContext.CDI);
		result.add(ThreadContext.SECURITY);
		result.add(ThreadContext.TRANSACTION);
		result.add(ThreadContext.APPLICATION);
		return result;
	}

	// For tests
	public List<ConcurrencyManagerExtension> getExtensions() {
		return extensions;
	}
}
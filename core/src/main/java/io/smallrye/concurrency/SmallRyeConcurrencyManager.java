package io.smallrye.concurrency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

import io.smallrye.concurrency.impl.ThreadContextImpl;
import io.smallrye.concurrency.impl.ThreadContextProviderPlan;
import io.smallrye.concurrency.spi.ThreadContextPropagator;

public class SmallRyeConcurrencyManager {
	
	private class Graph {
		Set<Node> roots = new HashSet<>();
		Map<String,Node> allNodes = new HashMap<>();
		
		void addRoot(ThreadContextProvider provider) {
			// ignore it as a root if it's already reachable
			if(allNodes.containsKey(provider.getThreadContextType()))
				return;
			Node node = new Node(provider);
			roots.add(node);
			node.init();
		}
		
		List<ThreadContextProvider> depthFirst(){
			ArrayList<ThreadContextProvider> ret = new ArrayList<>(allNodes.size());
			// FIXME: detect cycles
			for (Node root : roots) {
				root.depthFirst(ret);
			}
			return ret;
		}
		
		private class Node {
			private Map<String,Node> dependencies = new HashMap<>();
			private ThreadContextProvider provider;

			public Node(ThreadContextProvider provider) {
				this.provider = provider;
				allNodes.put(provider.getThreadContextType(), this);
			}

			public void depthFirst(ArrayList<ThreadContextProvider> ret) {
				if(ret.contains(provider))
					return;
				for (Node dep : dependencies.values()) {
					dep.depthFirst(ret);
				}
				ret.add(provider);
			}

			public void init() {
				for (String prerequisite : provider.getPrerequisites()) {
					add(prerequisite);
				}
			}

			private void add(String prerequisite) {
				Node req = allNodes.get(prerequisite);
				if(req == null) {
					// FIXME: handle missing provider
					req = new Node(providersByType.get(prerequisite));
					dependencies.put(prerequisite, req);
					req.init();
				} else {
					dependencies.put(prerequisite, req);
				}
			}

		}
	}

	public static final String[] NO_STRING = new String[0];
	
	private List<ThreadContextProvider> providers;
	private List<ThreadContextPropagator> propagators;
	private Map<String, ThreadContextProvider> providersByType;

	private String[] allProviderTypes;
	
	public SmallRyeConcurrencyManager() {
		this(toList(ServiceLoader.load(ThreadContextProvider.class)));
	}
	
	private static <T> List<T> toList(Iterable<T> iterable) {
		List<T> ret = new LinkedList<>();
		for (T t : iterable) {
			ret.add(t);
		}
		return ret;
	}

	// for tests
	SmallRyeConcurrencyManager(List<ThreadContextProvider> providers) {
		this.providers = new ArrayList<ThreadContextProvider>(providers);
		providersByType = new HashMap<>();
		for (ThreadContextProvider provider : providers) {
			providersByType.put(provider.getThreadContextType(), provider);
		}
		// FIXME: check for duplicate types
		// FIXME: check for cycles
		allProviderTypes = providersByType.keySet().toArray(new String[this.providers.size()]);
		ThreadContextImpl allThreadContext = new ThreadContextImpl(this, allProviderTypes, NO_STRING);
		propagators = new ArrayList<ThreadContextPropagator>();
		for (ThreadContextPropagator propagator : ServiceLoader.load(ThreadContextPropagator.class)) {
			propagators.add(propagator);
			propagator.setup(allThreadContext);
		}
	}
	
	public String[] getAllProviderTypes() {
		return allProviderTypes;
	}
	
	public CapturedContextState captureContext() {
		Map<String, String> props = Collections.emptyMap();
		return new CapturedContextState(this, getProviders(), props);
	}

	public CapturedContextState captureContext(String[] propagated, String[] unchanged) {
		Map<String, String> props = Collections.emptyMap();
		return new CapturedContextState(this, getProviders(propagated, unchanged), props);
	}

	// package-protected for tests
	ThreadContextProviderPlan getProviders() {
		return getProviders(allProviderTypes, NO_STRING);
	}
	
	// package-protected for tests
	ThreadContextProviderPlan getProviders(String[] propagated, String[] unchanged) {
		// FIXME: pretty sure there's an ALL in there to handle
		Graph propagatedGraph = new Graph();
		for (String type : propagated) {
			propagatedGraph.addRoot(providersByType.get(type));
		}
		List<ThreadContextProvider> propagatedProviders = propagatedGraph.depthFirst();

		Graph unchangedGraph = new Graph();
		for (String type : unchanged) {
			unchangedGraph.addRoot(providersByType.get(type));
		}
		List<ThreadContextProvider> unchangedProviders = unchangedGraph.depthFirst();
		
		// FIXME: error if both lists intersect
		Graph clearedGraph = new Graph();
		for (ThreadContextProvider provider : providers) {
			if(propagatedProviders.contains(provider)
					|| unchangedProviders.contains(provider))
				continue;
			clearedGraph.addRoot(provider);
		}
		List<ThreadContextProvider> clearedProviders = clearedGraph.depthFirst();
		
		// FIXME: error if clearedProviders appear in propagated or unchanged
		return new ThreadContextProviderPlan(propagatedProviders, clearedProviders);
	}
}

package io.smallrye.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.eclipse.microprofile.concurrent.spi.ConcurrencyManagerBuilder;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

import io.smallrye.concurrency.spi.ThreadContextPropagator;

public class SmallRyeConcurrencyManagerBuilder implements ConcurrencyManagerBuilder {

	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	private boolean addDiscoveredThreadContextProviders;
	private boolean addDiscoveredThreadContextPropagators;
    private List<ThreadContextProvider> contextProviders = new ArrayList<>();
    private List<ThreadContextPropagator> contextPropagators = new ArrayList<>();

	@Override
	public SmallRyeConcurrencyManagerBuilder withThreadContextProviders(ThreadContextProvider... providers) {
		for (ThreadContextProvider contextProvider : providers) {
			contextProviders.add(contextProvider);
		}
		return this;
	}

	@Override
	public SmallRyeConcurrencyManagerBuilder addDiscoveredThreadContextProviders() {
		addDiscoveredThreadContextProviders = true;
		return this;
	}

    private List<ThreadContextProvider> discoverThreadContextProviders() {
        List<ThreadContextProvider> discoveredThreadContextProviders = new ArrayList<>();
        ServiceLoader<ThreadContextProvider> configSourceLoader = ServiceLoader.load(ThreadContextProvider.class, classLoader);
        configSourceLoader.forEach(configSource -> {
            discoveredThreadContextProviders.add(configSource);
        });
        return discoveredThreadContextProviders;
    }

	public SmallRyeConcurrencyManagerBuilder withThreadContextPropagators(ThreadContextPropagator... propagators) {
		for (ThreadContextPropagator contextPropagator : propagators) {
			contextPropagators.add(contextPropagator);
		}
		return this;
	}

	public SmallRyeConcurrencyManagerBuilder addDiscoveredThreadContextPropagators() {
		addDiscoveredThreadContextPropagators = true;
		return this;
	}

    private List<ThreadContextPropagator> discoverThreadContextPropagators() {
        List<ThreadContextPropagator> discoveredThreadContextPropagators = new ArrayList<>();
        ServiceLoader<ThreadContextPropagator> configSourceLoader = ServiceLoader.load(ThreadContextPropagator.class, classLoader);
        configSourceLoader.forEach(configSource -> {
            discoveredThreadContextPropagators.add(configSource);
        });
        return discoveredThreadContextPropagators;
    }

	@Override
	public SmallRyeConcurrencyManagerBuilder forClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
		return this;
	}

	@Override
	public SmallRyeConcurrencyManager build() {
		if(addDiscoveredThreadContextProviders)
			contextProviders.addAll(discoverThreadContextProviders());
		if(addDiscoveredThreadContextPropagators)
			contextPropagators.addAll(discoverThreadContextPropagators());
		
		return new SmallRyeConcurrencyManager(contextProviders, contextPropagators);
	}

}

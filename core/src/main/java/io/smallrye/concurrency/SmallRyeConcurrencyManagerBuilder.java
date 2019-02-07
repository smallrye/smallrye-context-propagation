package io.smallrye.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.eclipse.microprofile.concurrent.spi.ConcurrencyManager;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyManagerExtension;
import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;

public class SmallRyeConcurrencyManagerBuilder implements ConcurrencyManager.Builder {

    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private boolean addDiscoveredThreadContextProviders;
    private boolean addDiscoveredConcurrencyManagerExtensions;
    private List<ThreadContextProvider> contextProviders = new ArrayList<>();
    private List<ConcurrencyManagerExtension> concurrencyManagerExtensions = new ArrayList<>();

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
        ServiceLoader<ThreadContextProvider> configSourceLoader = ServiceLoader.load(ThreadContextProvider.class,
                classLoader);
        configSourceLoader.forEach(configSource -> {
            discoveredThreadContextProviders.add(configSource);
        });
        return discoveredThreadContextProviders;
    }

    @Override
    public SmallRyeConcurrencyManagerBuilder withConcurrencyManagerExtensions(
            ConcurrencyManagerExtension... propagators) {
        for (ConcurrencyManagerExtension contextPropagator : propagators) {
            concurrencyManagerExtensions.add(contextPropagator);
        }
        return this;
    }

    @Override
    public SmallRyeConcurrencyManagerBuilder addDiscoveredConcurrencyManagerExtensions() {
        addDiscoveredConcurrencyManagerExtensions = true;
        return this;
    }

    private List<ConcurrencyManagerExtension> discoverConcurrencyManagerExtensions() {
        List<ConcurrencyManagerExtension> discoveredConcurrencyManagerExtensions = new ArrayList<>();
        ServiceLoader<ConcurrencyManagerExtension> configSourceLoader = ServiceLoader
                .load(ConcurrencyManagerExtension.class, classLoader);
        configSourceLoader.forEach(configSource -> {
            discoveredConcurrencyManagerExtensions.add(configSource);
        });
        return discoveredConcurrencyManagerExtensions;
    }

    @Override
    public SmallRyeConcurrencyManagerBuilder forClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @Override
    public SmallRyeConcurrencyManager build() {
        if (addDiscoveredThreadContextProviders)
            contextProviders.addAll(discoverThreadContextProviders());
        if (addDiscoveredConcurrencyManagerExtensions)
            concurrencyManagerExtensions.addAll(discoverConcurrencyManagerExtensions());

        return new SmallRyeConcurrencyManager(contextProviders, concurrencyManagerExtensions);
    }

}

package io.smallrye.context;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;

public class SmallRyeContextManagerBuilder implements ContextManager.Builder {

    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private boolean addDiscoveredThreadContextProviders;
    private boolean addDiscoveredContextManagerExtensions;
    private List<ThreadContextProvider> contextProviders = new ArrayList<>();
    private List<ContextManagerExtension> contextManagerExtensions = new ArrayList<>();

    @Override
    public SmallRyeContextManagerBuilder withThreadContextProviders(ThreadContextProvider... providers) {
        for (ThreadContextProvider contextProvider : providers) {
            contextProviders.add(contextProvider);
        }
        return this;
    }

    @Override
    public SmallRyeContextManagerBuilder addDiscoveredThreadContextProviders() {
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
    public SmallRyeContextManagerBuilder withContextManagerExtensions(
            ContextManagerExtension... propagators) {
        for (ContextManagerExtension contextPropagator : propagators) {
            contextManagerExtensions.add(contextPropagator);
        }
        return this;
    }

    @Override
    public SmallRyeContextManagerBuilder addDiscoveredContextManagerExtensions() {
        addDiscoveredContextManagerExtensions = true;
        return this;
    }

    private List<ContextManagerExtension> discoverContextManagerExtensions() {
        List<ContextManagerExtension> discoveredContextManagerExtensions = new ArrayList<>();
        ServiceLoader<ContextManagerExtension> configSourceLoader = ServiceLoader
                .load(ContextManagerExtension.class, classLoader);
        configSourceLoader.forEach(configSource -> {
            discoveredContextManagerExtensions.add(configSource);
        });
        return discoveredContextManagerExtensions;
    }

    @Override
    public SmallRyeContextManagerBuilder forClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @Override
    public SmallRyeContextManager build() {
        if (addDiscoveredThreadContextProviders)
            contextProviders.addAll(discoverThreadContextProviders());
        if (addDiscoveredContextManagerExtensions)
            contextManagerExtensions.addAll(discoverContextManagerExtensions());

        return new SmallRyeContextManager(contextProviders, contextManagerExtensions);
    }

}

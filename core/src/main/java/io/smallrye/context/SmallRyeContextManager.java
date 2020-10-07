package io.smallrye.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;

import io.smallrye.context.impl.CapturedContextState;
import io.smallrye.context.impl.DefaultValues;
import io.smallrye.context.impl.ThreadContextProviderPlan;

public class SmallRyeContextManager implements ContextManager {

    // newer version of MP config has to specify empty configuration by using a "None" string instead of empty string
    public static final String NONE = "None";

    public static final String[] NO_STRING = new String[0];

    public static final String[] ALL_REMAINING_ARRAY = new String[] { ThreadContext.ALL_REMAINING };

    private List<ThreadContextProvider> providers;
    private List<ContextManagerExtension> extensions;
    private Map<String, ThreadContextProvider> providersByType;
    private String[] allProviderTypes;
    private DefaultValues defaultValues;
    private ExecutorService defaultExecutorService;

    private SmallRyeThreadContext allPropagatedThreadContext;
    private SmallRyeThreadContext allClearedThreadContext;

    SmallRyeContextManager(List<ThreadContextProvider> providers, List<ContextManagerExtension> extensions,
            ExecutorService defaultExecutorService) {
        this.defaultExecutorService = defaultExecutorService;
        this.providers = new ArrayList<ThreadContextProvider>(providers);
        providersByType = new HashMap<>();
        for (ThreadContextProvider provider : providers) {
            String type = provider.getThreadContextType();
            // check for duplicate providers
            if (providersByType.containsKey(type))
                throw new IllegalStateException("ThreadContextProvider type already registered: " + type
                        + " first instance: " + providersByType.get(type) + ", second instance: " + provider);
            providersByType.put(type, provider);
        }
        allProviderTypes = providersByType.keySet().toArray(new String[this.providers.size()]);
        this.extensions = new ArrayList<ContextManagerExtension>(extensions);
        this.defaultValues = new DefaultValues();
        // Extensions may call our methods, so do all init before we call this
        for (ContextManagerExtension extension : extensions) {
            extension.setup(this);
        }
    }

    public String[] getAllProviderTypes() {
        return allProviderTypes;
    }

    public CapturedContextState captureContext(SmallRyeThreadContext context) {
        Map<String, String> props = Collections.emptyMap();
        return new CapturedContextState(context, context.getPlan(), props);
    }

    // for tests
    public ThreadContextProviderPlan getProviderPlan() {
        return getProviderPlan(allProviderTypes, NO_STRING, NO_STRING);
    }

    /**
     * Converts an array of context types into a set.
     * In the process it erases all occurrences of "None", treating them as empty String.
     * It also verifies that when "None" was used, no other context was specified.
     *
     * @param arrayOfContexts String array of contexts to be checked and transformed into a set
     * @return set of Strings representing contexts
     */
    private Set<String> createContextSetFromStringArray(String[] arrayOfContexts) {
        Set<String> result = new HashSet<>();
        boolean noneUsed = false;
        for (String context : arrayOfContexts) {
            if (context.equals(NONE)) {
                noneUsed = true;
                continue;
            } else {
                result.add(context);
            }
        }
        if (noneUsed && result.size() > 0) {
            throw new IllegalStateException("Cannot use 'None' in conjunction with any other contexts, the offending " +
                    "context declaration used: " + Arrays.toString(arrayOfContexts));
        }
        return result;
    }

    public ThreadContextProviderPlan getProviderPlan(String[] propagated, String[] unchanged, String[] cleared) {
        Set<String> propagatedSet = createContextSetFromStringArray(propagated);

        Set<String> clearedSet = createContextSetFromStringArray(cleared);

        Set<String> unchangedSet = createContextSetFromStringArray(unchanged);

        // check for duplicates
        if (propagatedSet.removeAll(unchangedSet) || propagatedSet.removeAll(clearedSet)
                || clearedSet.removeAll(propagatedSet) || clearedSet.removeAll(unchangedSet)
                || unchangedSet.removeAll(propagatedSet) || unchangedSet.removeAll(clearedSet)) {
            throw new IllegalStateException(
                    "Cannot use the same context in more than one of propagated (" + Arrays.toString(propagated)
                            + "), cleared (" + Arrays.toString(cleared) + "), unchanged (" + Arrays.toString(unchanged) + ")");
        }

        // expand ALL_REMAINING
        boolean hadAllRemaining = false;
        if (propagatedSet.contains(ThreadContext.ALL_REMAINING)) {
            propagatedSet.remove(ThreadContext.ALL_REMAINING);
            Collections.addAll(propagatedSet, allProviderTypes);
            propagatedSet.removeAll(clearedSet);
            propagatedSet.removeAll(unchangedSet);
            hadAllRemaining = true;
        }

        if (unchangedSet.contains(ThreadContext.ALL_REMAINING)) {
            unchangedSet.remove(ThreadContext.ALL_REMAINING);
            Collections.addAll(unchangedSet, allProviderTypes);
            unchangedSet.removeAll(propagatedSet);
            unchangedSet.removeAll(clearedSet);
            hadAllRemaining = true;
        }

        // cleared implicitly defaults to ALL_REMAINING if nobody else is using it
        if (clearedSet.contains(ThreadContext.ALL_REMAINING) || !hadAllRemaining) {
            clearedSet.remove(ThreadContext.ALL_REMAINING);
            Collections.addAll(clearedSet, allProviderTypes);
            clearedSet.removeAll(propagatedSet);
            clearedSet.removeAll(unchangedSet);
        }

        // check for existence
        Set<ThreadContextProvider> propagatedProviders = new HashSet<>();
        for (String type : propagatedSet) {
            if (type.isEmpty()) {
                continue;
            }
            ThreadContextProvider provider = providersByType.get(type);
            if (provider == null)
                throw new IllegalStateException("Missing propagated provider type: " + type);
            propagatedProviders.add(provider);
        }

        // ignore missing for cleared/unchanged
        Set<ThreadContextProvider> unchangedProviders = new HashSet<>();
        for (String type : unchangedSet) {
            if (type.isEmpty()) {
                continue;
            }
            ThreadContextProvider provider = providersByType.get(type);
            if (provider != null)
                unchangedProviders.add(provider);
        }

        Set<ThreadContextProvider> clearedProviders = new HashSet<>();
        for (String type : clearedSet) {
            if (type.isEmpty()) {
                continue;
            }
            ThreadContextProvider provider = providersByType.get(type);
            if (provider != null)
                clearedProviders.add(provider);
        }

        return new ThreadContextProviderPlan(propagatedProviders, unchangedProviders, clearedProviders);
    }

    @Override
    public SmallRyeManagedExecutor.Builder newManagedExecutorBuilder() {
        return new SmallRyeManagedExecutor.Builder(this);
    }

    @Override
    public SmallRyeThreadContext.Builder newThreadContextBuilder() {
        return new SmallRyeThreadContext.Builder(this);
    }

    public ExecutorService getDefaultExecutorService() {
        return defaultExecutorService;
    }

    // For tests
    public List<ContextManagerExtension> getExtensions() {
        return extensions;
    }

    public DefaultValues getDefaultValues() {
        return defaultValues;
    }

    //
    // Extras

    /**
     * Returns a {@link SmallRyeThreadContext} instance which propagates all thread contexts.
     * 
     * @return a {@link SmallRyeThreadContext} instance which propagates all thread contexts.
     */
    public SmallRyeThreadContext allPropagatedThreadContext() {
        // double parallel instantiation is not an issue
        if (allPropagatedThreadContext == null) {
            allPropagatedThreadContext = newThreadContextBuilder()
                    .propagated(ThreadContext.ALL_REMAINING)
                    .cleared()
                    .unchanged()
                    .build();
        }
        return allPropagatedThreadContext;
    }

    /**
     * Returns a {@link SmallRyeThreadContext} instance which clears all thread contexts.
     * 
     * @return a {@link SmallRyeThreadContext} instance which clears all thread contexts.
     */
    public SmallRyeThreadContext allClearedThreadContext() {
        // double parallel instantiation is not an issue
        if (allClearedThreadContext == null) {
            allClearedThreadContext = newThreadContextBuilder()
                    .propagated()
                    .cleared(ThreadContext.ALL_REMAINING)
                    .unchanged()
                    .build();
        }
        return allClearedThreadContext;
    }

    public static class Builder implements ContextManager.Builder {

        private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        private boolean addDiscoveredThreadContextProviders;
        private boolean addDiscoveredContextManagerExtensions;
        private List<ThreadContextProvider> contextProviders = new ArrayList<>();
        private List<ContextManagerExtension> contextManagerExtensions = new ArrayList<>();
        private ExecutorService defaultExecutorService;

        @Override
        public Builder withThreadContextProviders(ThreadContextProvider... providers) {
            for (ThreadContextProvider contextProvider : providers) {
                contextProviders.add(contextProvider);
            }
            return this;
        }

        @Override
        public Builder addDiscoveredThreadContextProviders() {
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
        public Builder withContextManagerExtensions(
                ContextManagerExtension... propagators) {
            for (ContextManagerExtension contextPropagator : propagators) {
                contextManagerExtensions.add(contextPropagator);
            }
            return this;
        }

        @Override
        public Builder addDiscoveredContextManagerExtensions() {
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
        public Builder forClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        @Override
        public SmallRyeContextManager build() {
            if (addDiscoveredThreadContextProviders)
                contextProviders.addAll(discoverThreadContextProviders());
            if (addDiscoveredContextManagerExtensions)
                contextManagerExtensions.addAll(discoverContextManagerExtensions());

            return new SmallRyeContextManager(contextProviders, contextManagerExtensions, defaultExecutorService);
        }

        //
        // Extras

        /**
         * Make all created {@link SmallRyeManagedExecutor} forward to the given executor service by default instead of
         * creating new executor services to back them. This can be overridden with
         * {@link SmallRyeManagedExecutor.Builder#withExecutorService(ExecutorService)} and
         * {@link SmallRyeManagedExecutor.Builder#withNewExecutorService()}. Also serves as the default executor to use
         * by all {@link CompletionStage} and {@link CompletableFuture} wrapped by {@link ThreadContext}.
         * 
         * @param executorService the executor service to delegate to. If <code>null</code>, all created {@link ManagedExecutor}
         *        will create new backing executor services, and all <code>*Async</code> methods of the
         *        {@link CompletionStage} and {@link CompletableFuture} wrapped by {@link ThreadContext} will throw due to a
         *        lack of executor.
         * @return this builder.
         */
        public Builder withDefaultExecutorService(ExecutorService executorService) {
            this.defaultExecutorService = executorService;
            return this;
        }
    }
}

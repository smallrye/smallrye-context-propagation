package io.smallrye.context.impl;

import io.smallrye.context.SmallRyeContextManager;
import io.smallrye.context.api.ManagedExecutorConfig;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Holds default values for {@code ManagedExecutor} and {@code ThreadContext}. It firstly looks into MP Config
 * for any user-specified defaults and if not defined, then it uses SmallRye defaults which propagate everything.
 *
 * @author Matej Novotny
 */
public class DefaultValues {

    // constants defined by spec for MP Config
    private final String EXEC_ASYNC = "mp.context.ManagedExecutor.maxAsync";
    private final String EXEC_QUEUE = "mp.context.ManagedExecutor.maxQueued";
    private final String EXEC_PROPAGATED = "mp.context.ManagedExecutor.propagated";
    private final String EXEC_CLEARED = "mp.context.ManagedExecutor.cleared";
    private final String THREAD_CLEARED = "mp.context.ThreadContext.cleared";
    private final String THREAD_PROPAGATED = "mp.context.ThreadContext.propagated";
    private final String THREAD_UNCHANGED = "mp.context.ThreadContext.unchanged";

    // actual defaults
    private String[] executorPropagated;
    private String[] executorCleared;
    private int executorAsync;
    private int executorQueue;
    private String[] threadPropagated;
    private String[] threadCleared;
    private String[] threadUnchanged;

    public DefaultValues() {
        // NOTE: we do not perform sanity check here, that's done in SmallRyeContextManager
        Config config = ConfigProvider.getConfig();
        Set<String> allkeys = new HashSet<>();
        config.getPropertyNames().forEach(item -> allkeys.add(item));
        this.executorAsync = config.getOptionalValue(EXEC_ASYNC, Integer.class)
                .orElse(ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxAsync());
        this.executorQueue = config.getOptionalValue(EXEC_QUEUE, Integer.class)
                .orElse(ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxQueued());
        // remaining values have to be done via try-catch block because SmallRye Config
        // considers key with empty value as non-existent
        // https://github.com/smallrye/smallrye-config/issues/83
        this.executorPropagated = resolveConfiguration(config, EXEC_PROPAGATED, SmallRyeContextManager.ALL_REMAINING_ARRAY, allkeys);
        this.executorCleared = resolveConfiguration(config, EXEC_CLEARED, SmallRyeContextManager.NO_STRING, allkeys);
        this.threadCleared = resolveConfiguration(config, THREAD_CLEARED, SmallRyeContextManager.NO_STRING, allkeys);
        this.threadPropagated = resolveConfiguration(config, THREAD_PROPAGATED, SmallRyeContextManager.ALL_REMAINING_ARRAY, allkeys);
        this.threadUnchanged = resolveConfiguration(config, THREAD_UNCHANGED, SmallRyeContextManager.NO_STRING, allkeys);
    }

    private String[] resolveConfiguration(Config mpConfig, String key, String[] originalValue, Set<String> allKeys) {
        try {
            return mpConfig.getValue(key, String[].class);
        } catch (NoSuchElementException e) {
            // check keys, there still might be a key with no value assigned
            if (allKeys.contains(key)) {
                return new String[]{};
            }
            return originalValue;
        }
    }

    public String[] getExecutorPropagated() {
        return executorPropagated;
    }

    public String[] getExecutorCleared() {
        return executorCleared;
    }

    public int getExecutorAsync() {
        return executorAsync;
    }

    public int getExecutorQueue() {
        return executorQueue;
    }

    public String[] getThreadPropagated() {
        return threadPropagated;
    }

    public String[] getThreadCleared() {
        return threadCleared;
    }

    public String[] getThreadUnchanged() {
        return threadUnchanged;
    }
}

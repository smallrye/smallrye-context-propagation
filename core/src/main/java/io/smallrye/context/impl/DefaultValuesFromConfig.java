package io.smallrye.context.impl;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.smallrye.context.SmallRyeContextManager;
import io.smallrye.context.api.ManagedExecutorConfig;

/**
 * Holds default values for {@code ManagedExecutor} and {@code ThreadContext}. It firstly looks into MP Config
 * for any user-specified defaults and if not defined, then it uses SmallRye defaults which propagate everything.
 *
 * @author Matej Novotny
 */
public class DefaultValuesFromConfig implements DefaultValues {

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

    public DefaultValuesFromConfig() {
        // NOTE: we do not perform sanity check here, that's done in SmallRyeContextManager
        Config config = ConfigProvider.getConfig();
        this.executorAsync = config.getOptionalValue(EXEC_ASYNC, Integer.class)
                .orElse(ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxAsync());
        this.executorQueue = config.getOptionalValue(EXEC_QUEUE, Integer.class)
                .orElse(ManagedExecutorConfig.Literal.DEFAULT_INSTANCE.maxQueued());
        this.executorPropagated = resolveConfiguration(config, EXEC_PROPAGATED, SmallRyeContextManager.ALL_REMAINING_ARRAY);
        this.executorCleared = resolveConfiguration(config, EXEC_CLEARED, SmallRyeContextManager.NO_STRING);
        this.threadCleared = resolveConfiguration(config, THREAD_CLEARED, SmallRyeContextManager.NO_STRING);
        this.threadPropagated = resolveConfiguration(config, THREAD_PROPAGATED, SmallRyeContextManager.ALL_REMAINING_ARRAY);
        this.threadUnchanged = resolveConfiguration(config, THREAD_UNCHANGED, SmallRyeContextManager.NO_STRING);
    }

    private String[] resolveConfiguration(Config mpConfig, String key, String[] originalValue) {
        Optional<String[]> value = mpConfig.getOptionalValue(key, String[].class);
        if (value.isPresent()) {
            return value.get();
        }
        // SmallRye Config considers a key with an empty value as non-existent
        // (https://github.com/smallrye/smallrye-config/issues/83), and such a key means "nothing" rather than the
        // default: the raw value tells whether the key is defined without an exception or a scan of all the names
        if (mpConfig.getConfigValue(key).getValue() != null) {
            return new String[] {};
        }
        return originalValue;
    }

    @Override
    public String[] getExecutorPropagated() {
        return executorPropagated;
    }

    @Override
    public String[] getExecutorCleared() {
        return executorCleared;
    }

    @Override
    public int getExecutorAsync() {
        return executorAsync;
    }

    @Override
    public int getExecutorQueue() {
        return executorQueue;
    }

    @Override
    public String[] getThreadPropagated() {
        return threadPropagated;
    }

    @Override
    public String[] getThreadCleared() {
        return threadCleared;
    }

    @Override
    public String[] getThreadUnchanged() {
        return threadUnchanged;
    }
}

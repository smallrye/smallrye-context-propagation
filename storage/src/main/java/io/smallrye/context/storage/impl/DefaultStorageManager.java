package io.smallrye.context.storage.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.context.storage.spi.StorageDeclaration;
import io.smallrye.context.storage.spi.StorageManager;

/**
 * Default implementation which allocates new regular ThreadLocal for any storage declaration class.
 */
public class DefaultStorageManager implements StorageManager {

    private final Map<Class<?>, ThreadLocal<?>> threadLocals = new ConcurrentHashMap<>();

    /**
     * Returns a regular ThreadLocal specific for the given storage declaration.
     */
    @Override
    public <T extends StorageDeclaration<X>, X> ThreadLocal<X> getThreadLocal(Class<T> klass) {
        return (ThreadLocal<X>) threadLocals.computeIfAbsent(klass, v -> new ThreadLocal<>());
    }
}

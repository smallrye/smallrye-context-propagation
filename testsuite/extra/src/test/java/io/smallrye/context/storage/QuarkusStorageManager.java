package io.smallrye.context.storage;

import io.smallrye.context.impl.SmallRyeThreadContextStorageDeclaration;
import io.smallrye.context.storage.impl.ThreadLocalScope;
import io.smallrye.context.storage.spi.StorageDeclaration;
import io.smallrye.context.storage.spi.StorageManager;
import io.smallrye.context.storage.spi.ThreadScope;

/**
 * To be implemented in Quarkus
 */
class QuarkusStorageManager implements StorageManager {

    private final static ThreadScope<?> resteasyStorage = new ThreadLocalScope<>(new RESTEasy_QuarkusStorage());
    private final static ThreadScope<?> threadContextStorage = new ThreadLocalScope<>(
            new SmallRyeThreadContext_QuarkusStorage());

    /**
     * This part will be generated depending on the discovered StorageUsers
     */
    @Override
    public <T extends StorageDeclaration<X>, X> ThreadScope<X> getThreadScope(Class<T> klass) {
        if (klass == RESTEasyStorageDeclaration.class)
            return (ThreadScope<X>) resteasyStorage;
        if (klass == SmallRyeThreadContextStorageDeclaration.class)
            return (ThreadScope<X>) threadContextStorage;
        throw new IllegalArgumentException("Storage user nor registered: " + klass);
    }

    public static QuarkusStorageManager instance() {
        return (QuarkusStorageManager) StorageManager.instance();
    }

    public Object[] newContext() {
        return new Object[2];
    }
}
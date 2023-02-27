package io.smallrye.context.storage;

import io.smallrye.context.impl.SmallRyeThreadContextStorageDeclaration;
import io.smallrye.context.storage.spi.StorageDeclaration;
import io.smallrye.context.storage.spi.StorageManager;

/**
 * To be implemented in Quarkus
 */
class QuarkusStorageManager implements StorageManager {

    private final static ThreadLocal<?> resteasyStorage = new RESTEasy_QuarkusStorage();
    private final static ThreadLocal<?> threadContextStorage = new SmallRyeThreadContext_QuarkusStorage();

    /**
     * This part will be generated depending on the discovered StorageUsers
     */
    @Override
    public <T extends StorageDeclaration<X>, X> ThreadLocal<X> getThreadLocal(Class<T> klass) {
        if (klass == RESTEasyStorageDeclaration.class)
            return (ThreadLocal<X>) resteasyStorage;
        if (klass == SmallRyeThreadContextStorageDeclaration.class)
            return (ThreadLocal<X>) threadContextStorage;
        throw new IllegalArgumentException("Storage user nor registered: " + klass);
    }

    public static QuarkusStorageManager instance() {
        return (QuarkusStorageManager) StorageManager.instance();
    }

    public Object[] newContext() {
        return new Object[2];
    }
}
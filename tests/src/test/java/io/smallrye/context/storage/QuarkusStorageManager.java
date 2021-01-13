package io.smallrye.context.storage;

import io.smallrye.context.impl.SmallRyeThreadContextStorageDeclaration;
import io.smallrye.context.storage.spi.StorageDeclaration;
import io.smallrye.context.storage.spi.StorageManager;

/**
 * To be implemented in Quarkus
 */
class QuarkusStorageManager implements StorageManager {

    /**
     * This part will be generated depending on the discovered StorageUsers
     */
    @Override
    public <T extends StorageDeclaration<X>, X> ThreadLocal<X> allocateThreadLocal(Class<T> klass) {
        if (klass == RESTEasyStorageDeclaration.class)
            return (ThreadLocal<X>) new RESTEasy_QuarkusStorage();
        if (klass == SmallRyeThreadContextStorageDeclaration.class)
            return (ThreadLocal<X>) new SmallRyeThreadContext_QuarkusStorage();
        throw new IllegalArgumentException("Storage user nor registered: " + klass);
    }

    public static QuarkusStorageManager instance() {
        return (QuarkusStorageManager) StorageManager.instance();
    }

    public QuarkusThreadContext newContext() {
        return new QuarkusThreadContextImpl();
    }
}
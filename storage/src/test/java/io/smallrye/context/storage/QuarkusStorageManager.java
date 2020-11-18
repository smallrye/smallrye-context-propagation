package io.smallrye.context.storage;

import io.smallrye.context.storage.spi.StorageManager;
import io.smallrye.context.storage.spi.StorageSlot;

/**
 * To be implemented in Quarkus
 */
class QuarkusStorageManager implements StorageManager {

    /**
     * This part will be generated depending on the discovered StorageUsers
     */
    @Override
    public <T extends ThreadLocalStorage<X>, X> StorageSlot<X> allocateStorageSlot(Class<T> klass) {
        if (klass.getName().equals("io.smallrye.context.storage.RESTEasyContext$1"))
            return (StorageSlot<X>) new RESTEasy_QuarkusStorage();
        throw new IllegalArgumentException("Storage user nor registered: " + klass);
    }

    public static QuarkusStorageManager instance() {
        return (QuarkusStorageManager) StorageManager.instance();
    }

    public QuarkusThreadContext newContext() {
        return new QuarkusThreadContextImpl();
    }
}
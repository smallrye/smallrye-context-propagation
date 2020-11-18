package io.smallrye.context.storage.spi;

import io.smallrye.context.storage.ThreadLocalStorage;

/**
 * This is what Quarkus will implement to distribute StorageSlot objects to RESTEasy, ArC, Narayana
 */
public interface StorageManager {

    public static StorageManager instance() {
        return StorageManagerProvider.instance().getStorageManager();
    }

    /**
     * Returns a new Storage for the given registered StorageRequirement.
     */
    public <T extends ThreadLocalStorage<X>, X> StorageSlot<X> allocateStorageSlot(Class<T> threadLocalStorageClass);
}

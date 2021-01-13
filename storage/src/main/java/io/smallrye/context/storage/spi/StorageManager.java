package io.smallrye.context.storage.spi;

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
    public <T extends StorageDeclaration<X>, X> ThreadLocal<X> allocateThreadLocal(Class<T> threadLocalStorageClass);

    public static <T extends StorageDeclaration<X>, X> ThreadLocal<X> threadLocal(Class<T> threadLocalStorageClass) {
        return instance().allocateThreadLocal(threadLocalStorageClass);
    }
}

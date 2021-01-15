package io.smallrye.context.storage.spi;

/**
 * This is what Quarkus will implement to distribute ThreadLocal objects to RESTEasy, ArC, Narayana
 */
public interface StorageManager {

    public static StorageManager instance() {
        return StorageManagerProvider.instance().getStorageManager();
    }

    /**
     * Returns a new ThreadLocal for the given registered StorageDeclaration.
     */
    public <T extends StorageDeclaration<X>, X> ThreadLocal<X> getThreadLocal(Class<T> threadLocalStorageClass);

    public static <T extends StorageDeclaration<X>, X> ThreadLocal<X> threadLocal(Class<T> threadLocalStorageClass) {
        return instance().getThreadLocal(threadLocalStorageClass);
    }
}

package io.smallrye.context.storage.spi;

/**
 * Allows libraries to obtain custom-generated ThreadLocal instances for their storage.
 */
public interface StorageManager {

    /**
     * Returns the currently registered StorageManager
     *
     * @return the currently registered StorageManager
     */
    static StorageManager instance() {
        return StorageManagerProvider.instance().getStorageManager();
    }

    /**
     * Obtains a ThreadLocal suitable for the given registered StorageDeclaration. The ThreadLocal
     * may be a regular ThreadLocal, or one that this StorageManager manufactured for special dedicated
     * storage. The returned ThreadLocal is cached and all subsequent calls to this method with the same
     * storage declaration will always return the same ThreadLocal instance.
     *
     * @param storageDeclarationClass the declaration class which defines the type of item we want to store in the
     *        required ThreadLocal
     * @return the ThreadLocal, memoized
     * @param <T> The StorageDeclaration type
     * @param <X> The type of data we store in that ThreadLocal
     */
    <T extends StorageDeclaration<X>, X> ThreadScope<X> getThreadScope(Class<T> storageDeclarationClass);

    /**
     * Obtains a ThreadLocal suitable for the given registered StorageDeclaration. The ThreadLocal
     * may be a regular ThreadLocal, or one that this StorageManager manufactured for special dedicated
     * storage. The returned ThreadLocal is cached and all subsequent calls to this method with the same
     * storage declaration will always return the same ThreadLocal instance.
     *
     * @param storageDeclarationClass the declaration class which defines the type of item we want to store in the
     *        required ThreadLocal
     * @return the ThreadLocal, memoized
     * @param <T> The StorageDeclaration type
     * @param <X> The type of data we store in that ThreadLocal
     */
    static <T extends StorageDeclaration<X>, X> ThreadScope<X> threadScope(Class<T> storageDeclarationClass) {
        return instance().getThreadScope(storageDeclarationClass);
    }
}

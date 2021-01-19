package io.smallrye.context.storage.spi;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.context.storage.impl.DefaultStorageManagerProvider;

/**
 * SPI to register custom StorageManager implementations
 */
public interface StorageManagerProvider {

    static AtomicReference<StorageManagerProvider> INSTANCE = new AtomicReference<StorageManagerProvider>();

    /**
     * Returns the currently registered StorageManagerProvider. Will attempt to instantiate one based on
     * the ServiceLoader for StorageManagerProvider if it is not set. Will default to DefaultStorageManagerProvider
     * otherwise.
     * 
     * @return the currently registered StorageManagerProvider, lazily created.
     */
    public static StorageManagerProvider instance() {
        StorageManagerProvider provider = INSTANCE.get();
        if (provider == null) {
            for (StorageManagerProvider serviceProvider : ServiceLoader.load(StorageManagerProvider.class)) {
                if (INSTANCE.compareAndSet(null, serviceProvider)) {
                    provider = serviceProvider;
                } else {
                    throw new IllegalStateException("StorageManagerProvider already set");
                }
            }
            if (provider == null) {
                provider = new DefaultStorageManagerProvider();
            }
        }
        return provider;
    }

    /**
     * Registers and existing StorageManagerProvider
     * 
     * @param provider the provider to register
     * @return a registration object allowing you to unregister it
     * @throws IllegalStateException when there already is a registered provider
     */
    public static StorageManagerProviderRegistration register(StorageManagerProvider provider) throws IllegalStateException {
        if (INSTANCE.compareAndSet(null, provider)) {
            return new StorageManagerProviderRegistration(provider);
        } else {
            throw new IllegalStateException("A StorageManagerProvider implementation has already been registered.");
        }
    }

    /**
     * @return the current StorageManager, for the current TCCL
     */
    public default StorageManager getStorageManager() {
        ClassLoader loader = System.getSecurityManager() == null
                ? Thread.currentThread().getContextClassLoader()
                : AccessController
                        .doPrivileged((PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
        return getStorageManager(loader);
    }

    /**
     * Obtain the StorageManager registered for the given ClassLoader
     * 
     * @param classloader the classloader to use for looking up the StorageManager
     * @return the StorageManager registered for the given ClassLoader
     */
    public StorageManager getStorageManager(ClassLoader classloader);
}

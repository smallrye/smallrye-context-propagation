package io.smallrye.context.storage.spi;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.context.storage.impl.DefaultStorageManagerProvider;

/**
 * This is where Quarkus registers it has a StorageManager.
 */
public interface StorageManagerProvider {

    static AtomicReference<StorageManagerProvider> INSTANCE = new AtomicReference<StorageManagerProvider>();

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

    public static StorageManagerProviderRegistration register(StorageManagerProvider provider) throws IllegalStateException {
        if (INSTANCE.compareAndSet(null, provider)) {
            return new StorageManagerProviderRegistration(provider);
        } else {
            throw new IllegalStateException("A StorageManagerProvider implementation has already been registered.");
        }
    }

    public default StorageManager getStorageManager() {
        ClassLoader loader = System.getSecurityManager() == null
                ? Thread.currentThread().getContextClassLoader()
                : AccessController
                        .doPrivileged((PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
        return getStorageManager(loader);
    }

    public StorageManager getStorageManager(ClassLoader classloader);

    public default void registerStorageManager(StorageManager manager, ClassLoader classLoader) {
        throw new UnsupportedOperationException();
    }

    public default void releaseStorageManager(StorageManager manager) {
        throw new UnsupportedOperationException();
    }
}

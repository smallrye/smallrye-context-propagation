package io.smallrye.context.storage.spi;

/**
 * Represents a StorageManagerProvider registration, allowing you to unregister it.
 */
public class StorageManagerProviderRegistration {
    private final StorageManagerProvider provider;

    StorageManagerProviderRegistration(StorageManagerProvider provider) {
        this.provider = provider;
    }

    /**
     * Unregisters this StorageManagerProvider
     */
    public void unregister() {
        StorageManagerProvider.INSTANCE.compareAndSet(provider, null);
    }
}
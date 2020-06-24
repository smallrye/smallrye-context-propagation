package io.smallrye.context.storage;

public class StorageManagerProviderRegistration {
    private final StorageManagerProvider provider;

    StorageManagerProviderRegistration(StorageManagerProvider provider) {
        this.provider = provider;
    }

    public void unregister() {
        StorageManagerProvider.INSTANCE.compareAndSet(provider, null);
    }
}
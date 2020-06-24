package io.smallrye.context.storage;

public class DefaultStorageManagerProvider implements StorageManagerProvider {

    private static final StorageManager DefaultStorageManager = new DefaultStorageManager();

    @Override
    public StorageManager getStorageManager(ClassLoader classloader) {
        return DefaultStorageManager;
    }

}

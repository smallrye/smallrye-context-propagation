package io.smallrye.context.storage.impl;

import io.smallrye.context.storage.spi.StorageManager;
import io.smallrye.context.storage.spi.StorageManagerProvider;

public class DefaultStorageManagerProvider implements StorageManagerProvider {

    private static final StorageManager DefaultStorageManager = new DefaultStorageManager();

    @Override
    public StorageManager getStorageManager(ClassLoader classloader) {
        return DefaultStorageManager;
    }

}

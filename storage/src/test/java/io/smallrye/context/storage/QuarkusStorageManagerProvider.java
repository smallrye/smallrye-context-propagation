package io.smallrye.context.storage;

import io.smallrye.context.storage.spi.StorageManager;
import io.smallrye.context.storage.spi.StorageManagerProvider;

/**
 * To be implemented in Quarkus
 */
class QuarkusStorageManagerProvider implements StorageManagerProvider {

    @Override
    public StorageManager getStorageManager(ClassLoader cl) {
        return new QuarkusStorageManager();
    }

}
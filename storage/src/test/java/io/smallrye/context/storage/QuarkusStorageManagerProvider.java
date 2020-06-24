package io.smallrye.context.storage;

/**
 * To be implemented in Quarkus
 */
class QuarkusStorageManagerProvider implements StorageManagerProvider {

    @Override
    public StorageManager getStorageManager(ClassLoader cl) {
        return new QuarkusStorageManager();
    }

}
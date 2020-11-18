package io.smallrye.context.storage;

/**
 * To be implemented in Quarkus
 */
class QuarkusStorageManager implements StorageManager {

    /**
     * This part will be generated depending on the discovered StorageUsers
     */
    @Override
    public <T extends StorageRequirement<X>, X> Storage<X> allocateStorage(Class<T> klass) {
        if (klass.isAssignableFrom(RESTEasyContextStorageRequirement.class))
            return (Storage<X>) new RESTEasy_QuarkusStorage();
        throw new IllegalArgumentException("Storage user nor registered: " + klass);
    }

    public static QuarkusStorageManager instance() {
        return (QuarkusStorageManager) StorageManager.instance();
    }

    public QuarkusThreadContext newContext() {
        return new QuarkusThreadContextImpl();
    }
}
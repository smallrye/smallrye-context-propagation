package io.smallrye.context.storage;

/**
 * This is what Quarkus will implement to distribute Storage objects to RESTEasy, ArC, Narayana
 */
public interface StorageManager {

    public static StorageManager instance() {
        return StorageManagerProvider.instance().getStorageManager();
    }

    /**
     * Returns a new Storage for the given registered StorageRequirement.
     */
    public <T extends StorageRequirement<X>, X> Storage<X> allocateStorage(Class<T> klass);

    public interface Builder {
        public Builder withStorageUsers(StorageRequirement<?>... providers);

        public Builder addDiscoveredStorageUsers();

        public StorageManager build();
    }

}

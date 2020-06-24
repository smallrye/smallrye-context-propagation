package io.smallrye.context.storage;

public class DefaultStorageManager implements StorageManager {

    @Override
    public <T extends StorageRequirement<X>, X> Storage<X> allocateStorage(Class<T> klass) {
        return new ThreadLocalStorage<>();
    }
}

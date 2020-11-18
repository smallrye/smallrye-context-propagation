package io.smallrye.context.storage.impl;

import io.smallrye.context.storage.ThreadLocalStorage;
import io.smallrye.context.storage.spi.StorageManager;
import io.smallrye.context.storage.spi.StorageSlot;
import io.smallrye.context.storage.spi.ThreadLocalStorageSlot;

public class DefaultStorageManager implements StorageManager {

    @Override
    public <T extends ThreadLocalStorage<X>, X> StorageSlot<X> allocateStorageSlot(Class<T> klass) {
        return new ThreadLocalStorageSlot<>();
    }
}

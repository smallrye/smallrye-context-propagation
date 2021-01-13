package io.smallrye.context.storage.impl;

import io.smallrye.context.storage.spi.StorageDeclaration;
import io.smallrye.context.storage.spi.StorageManager;

public class DefaultStorageManager implements StorageManager {

    @Override
    public <T extends StorageDeclaration<X>, X> ThreadLocal<X> allocateThreadLocal(Class<T> klass) {
        return new ThreadLocal<>();
    }
}

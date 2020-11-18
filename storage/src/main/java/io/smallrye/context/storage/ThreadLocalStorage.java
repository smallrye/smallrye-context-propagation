package io.smallrye.context.storage;

import io.smallrye.context.storage.spi.StorageManager;
import io.smallrye.context.storage.spi.StorageSlot;

public abstract class ThreadLocalStorage<T> {

    private StorageSlot<T> slot;

    public ThreadLocalStorage() {
        StorageManager sm = StorageManager.instance();
        this.slot = sm.allocateStorageSlot(getClass());
    }

    public T get() {
        return slot.get();
    }

    public void set(T t) {
        slot.set(t);
    }

    public void remove() {
        slot.remove();
    }
}

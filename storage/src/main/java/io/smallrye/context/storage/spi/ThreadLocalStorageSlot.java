package io.smallrye.context.storage.spi;

/**
 * Storage class used when there's no StorageManager, delegates to a ThreadLocal
 */
public class ThreadLocalStorageSlot<T> implements StorageSlot<T> {

    protected final ThreadLocal<T> threadLocal = new ThreadLocal<>();

    @Override
    public T get() {
        return threadLocal.get();
    }

    @Override
    public void set(T t) {
        threadLocal.set(t);
    }

    @Override
    public void remove() {
        threadLocal.remove();
    }

}
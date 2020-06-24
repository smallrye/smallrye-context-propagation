package io.smallrye.context.storage;

/**
 * Storage class used when there's no StorageManager, delegates to a ThreadLocal
 */
public class ThreadLocalStorage<T> implements Storage<T> {

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
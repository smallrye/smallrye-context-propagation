package io.smallrye.context.storage.spi;

/**
 * Our new ThreadLocal, which RESTEasy, ArC, Narayana can use as a ThreadLocal, but which
 * can be backed by the QuarkusThread field, or a ThreadLocal if not running on a QuarkusThread.
 */
public interface StorageSlot<T> {
    public T get();

    public void set(T t);

    public void remove();
}

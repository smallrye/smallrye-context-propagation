package io.smallrye.context;

/**
 * AutoCloseable interface which doesn't throw.
 */
public interface CleanAutoCloseable<T> extends AutoCloseable {

    T callNoChecked();

    /**
     * Close this resource, no exception thrown.
     */
    void close();
}

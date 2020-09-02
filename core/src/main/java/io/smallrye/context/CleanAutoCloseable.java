package io.smallrye.context;

/**
 * AutoCloseable interface which doesn't throw.
 */
@FunctionalInterface
public interface CleanAutoCloseable extends AutoCloseable {
    /**
     * Close this resource, no exception thrown.
     */
    void close();
}

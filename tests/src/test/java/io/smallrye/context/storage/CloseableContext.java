package io.smallrye.context.storage;

/**
 * Convenience class for faking ScopedLocals
 */
public interface CloseableContext extends AutoCloseable {
    @Override
    public void close();
}

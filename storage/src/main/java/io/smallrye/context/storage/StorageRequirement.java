package io.smallrye.context.storage;

/**
 * SPI interface to be implemented by every lib which needs a ThreadLocal field on QuarkusThread
 * so we know its type and the name of the field.
 */
public interface StorageRequirement<T> {
    public String name();
}

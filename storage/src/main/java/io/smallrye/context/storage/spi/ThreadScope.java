package io.smallrye.context.storage.spi;

public interface ThreadScope<T> {

    T get();

    void set(T value);

    void remove();
}

package io.smallrye.context.storage.impl;

import io.smallrye.context.storage.spi.ThreadScope;

public class ThreadLocalScope<T> implements ThreadScope<T> {

    protected final ThreadLocal<T> threadLocal;

    public ThreadLocalScope() {
        this(new ThreadLocal<>());
    }

    public ThreadLocalScope(ThreadLocal<T> threadLocal) {
        this.threadLocal = threadLocal;
    }

    @Override
    public T get() {
        return threadLocal.get();
    }

    @Override
    public void set(T value) {
        threadLocal.set(value);
    }

    @Override
    public void remove() {
        threadLocal.remove();
    }
}

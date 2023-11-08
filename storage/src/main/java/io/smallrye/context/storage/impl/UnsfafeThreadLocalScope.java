package io.smallrye.context.storage.impl;

public class UnsafeThreadLocalScope<T> extends ThreadLocalScope<T> {

    public UnsfafeThreadLocalScope() {
        super(new ThreadLocal<>());
    }

    public UnsfafeThreadLocalScope(ThreadLocal<T> threadLocal) {
        super(threadLocal);
    }

    @Override
    public void remove() {
        // Implement a faster remove that however could cause memory leaks in some cases
        // see https://github.com/spring-cloud/spring-cloud-sleuth/issues/27
        threadLocal.set(null);
    }
}

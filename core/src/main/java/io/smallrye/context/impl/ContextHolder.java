package io.smallrye.context.impl;

public interface ContextHolder {

    void captureThreadLocal(int index, ThreadLocal<Object> threadLocal, Object value);

}

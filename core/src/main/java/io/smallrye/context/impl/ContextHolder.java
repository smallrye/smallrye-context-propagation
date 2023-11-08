package io.smallrye.context.impl;

import io.smallrye.context.storage.spi.ThreadScope;

/**
 * Interface to be implemented by contextual wrappers so the plan can feed them thread locals.
 */
public interface ContextHolder {

    /**
     * Store a thread local and its current value while capturing, in a way that storage is flattend in the context wrapper with
     * minimal allocation.
     *
     * @param index the context provider index
     * @param threadLocal the context provider's threadLocal
     * @param value the current or cleared value of the threadLocal (depending on ThreadContext settings)
     */
    void captureThreadScope(int index, ThreadScope<Object> threadLocal, Object value);

}

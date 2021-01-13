package io.smallrye.context.storage;

import io.smallrye.context.SmallRyeThreadContext;

/**
 * This class is generated based on the discovery of SmallRyeThreadContext
 */
class SmallRyeThreadContext_QuarkusStorage extends ThreadLocal<SmallRyeThreadContext> {

    @Override
    public SmallRyeThreadContext get() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QuarkusThread) {
            return ((QuarkusThreadContextImpl) ((QuarkusThread) currentThread).getQuarkusThreadContext()).smallRyeThreadContext;
        } else {
            return super.get();
        }
    }

    @Override
    public void set(SmallRyeThreadContext t) {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QuarkusThread) {
            ((QuarkusThreadContextImpl) ((QuarkusThread) currentThread).getQuarkusThreadContext()).smallRyeThreadContext = t;
        } else {
            super.set(t);
        }
    }

    @Override
    public void remove() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QuarkusThread) {
            ((QuarkusThreadContextImpl) ((QuarkusThread) currentThread).getQuarkusThreadContext()).smallRyeThreadContext = null;
        } else {
            super.remove();
        }
    }
}
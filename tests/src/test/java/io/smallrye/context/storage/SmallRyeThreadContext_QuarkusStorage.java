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
            return (SmallRyeThreadContext) ((QuarkusThread) currentThread).getQuarkusThreadContext()[1];
        } else {
            return super.get();
        }
    }

    @Override
    public void set(SmallRyeThreadContext t) {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QuarkusThread) {
            ((QuarkusThread) currentThread).getQuarkusThreadContext()[1] = t;
        } else {
            super.set(t);
        }
    }

    @Override
    public void remove() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QuarkusThread) {
            ((QuarkusThread) currentThread).getQuarkusThreadContext()[1] = null;
        } else {
            super.remove();
        }
    }
}
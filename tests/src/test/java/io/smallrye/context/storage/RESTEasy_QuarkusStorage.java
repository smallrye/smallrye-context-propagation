package io.smallrye.context.storage;

import java.util.List;
import java.util.Map;

/**
 * This class is generated based on the discovery of RESTEasyContextStorageRequirement
 */
class RESTEasy_QuarkusStorage extends ThreadLocal<List<Map<Class<?>, Object>>> {

    @Override
    public List<Map<Class<?>, Object>> get() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QuarkusThread) {
            return ((QuarkusThreadContextImpl) ((QuarkusThread) currentThread).getQuarkusThreadContext()).resteasy;
        } else {
            return super.get();
        }
    }

    @Override
    public void set(List<Map<Class<?>, Object>> t) {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QuarkusThread) {
            ((QuarkusThreadContextImpl) ((QuarkusThread) currentThread).getQuarkusThreadContext()).resteasy = t;
        } else {
            super.set(t);
        }
    }

    @Override
    public void remove() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QuarkusThread) {
            ((QuarkusThreadContextImpl) ((QuarkusThread) currentThread).getQuarkusThreadContext()).resteasy = null;
        } else {
            super.remove();
        }
    }
}
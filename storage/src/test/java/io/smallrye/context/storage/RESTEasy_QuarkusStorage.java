package io.smallrye.context.storage;

import java.util.List;
import java.util.Map;

/**
 * This class is generated based on the discovery of RESTEasyContextStorageRequirement
 */
class RESTEasy_QuarkusStorage extends ThreadLocalStorage<List<Map<Class<?>, Object>>> {

    @Override
    public List<Map<Class<?>, Object>> get() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QuarkusThread) {
            return ((QuarkusContextsImpl) ((QuarkusThread) currentThread).contexts).resteasy;
        } else {
            return threadLocal.get();
        }
    }

    @Override
    public void set(List<Map<Class<?>, Object>> t) {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QuarkusThread) {
            ((QuarkusContextsImpl) ((QuarkusThread) currentThread).contexts).resteasy = t;
        } else {
            threadLocal.set(t);
        }
    }

    @Override
    public void remove() {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QuarkusThread) {
            ((QuarkusContextsImpl) ((QuarkusThread) currentThread).contexts).resteasy = null;
        } else {
            threadLocal.remove();
        }
    }
}
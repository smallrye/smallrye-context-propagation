package io.smallrye.context.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a fake version of ResteasyContext from RESTEasy, where it stores its ThreadLocal Stack of Map contexts.
 * I use this to validate our API without modifying RESTEasy for now.
 */
class RESTEasyContext {

    // this gives us storage for our List<Map<Class<?>, Object>> which we declared in RESTEasyContextStorage
    static final Storage<List<Map<Class<?>, Object>>> context = StorageManagerProvider.instance().getStorageManager()
            .allocateStorage(RESTEasyContextStorageRequirement.class);

    static <T> T getContext(Class<T> klass) {
        Map<Class<?>, Object> context = getContext();
        if (context == null)
            return null;
        return (T) context.get(klass);
    }

    public static CloseableContext pushContextLevelProper(Map<Class<?>, Object> map) {
        pushContextLevel(map);
        return () -> removeContextLevel();
    }

    static <T> void pushContext(Class<T> klass, Object value) {
        getContext().put(klass, value);
    }

    static void newContextLevel() {
        pushContextLevel(new HashMap<>());
    }

    static void pushContextLevel(Map<Class<?>, Object> map) {
        List<Map<Class<?>, Object>> contexts = context.get();
        if (contexts == null) {
            contexts = new ArrayList<>();
            context.set(contexts);
        }
        // no context is an empty map
        contexts.add(map != null ? map : new HashMap<>());
    }

    static Map<Class<?>, Object> getContext() {
        List<Map<Class<?>, Object>> contexts = context.get();
        if (contexts == null || contexts.isEmpty()) {
            return null;
        }
        return contexts.get(contexts.size() - 1);
    }

    static void removeContextLevel() {
        List<Map<Class<?>, Object>> contexts = context.get();
        if (contexts != null && !contexts.isEmpty()) {
            contexts.remove(contexts.size() - 1);
        }
    }

    static CloseableContext newContextLevelProper() {
        newContextLevel();
        return () -> removeContextLevel();
    }
}
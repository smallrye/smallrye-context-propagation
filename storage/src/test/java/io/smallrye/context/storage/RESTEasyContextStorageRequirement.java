package io.smallrye.context.storage;

import java.util.List;
import java.util.Map;

/**
 * This declares that we'll need Storage for RESTEasy's List<Map<Class<?>, Object>>
 */
class RESTEasyContextStorageRequirement implements StorageRequirement<List<Map<Class<?>, Object>>> {

    @Override
    public String name() {
        return "resteasy";
    }

}
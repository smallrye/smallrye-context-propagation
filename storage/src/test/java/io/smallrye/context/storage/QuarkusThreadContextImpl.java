package io.smallrye.context.storage;

import java.util.List;
import java.util.Map;

/**
 * This class is generated by Quarkus depending on the registered StorageUsers
 */
public class QuarkusThreadContextImpl implements QuarkusThreadContext {
    // this comes from RESTEasyContextStorageRequirement.name() and its type parameter
    List<Map<Class<?>, Object>> resteasy;

    // Experimental: this is only required if QuarkusStorageThreadContext pans out
    @Override
    public QuarkusThreadContext copy() {
        QuarkusThreadContextImpl ret = new QuarkusThreadContextImpl();
        ret.resteasy = this.resteasy;
        return ret;
    }

    public static class Factory implements QuarkusThreadContext.Factory {

        @Override
        public QuarkusThreadContext newContext() {
            return new QuarkusThreadContextImpl();
        }

    }
}
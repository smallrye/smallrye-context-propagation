package io.smallrye.context.storage;

/**
 * Marker interface to be stuck into the QuarkusThread
 */
public interface QuarkusThreadContext {

    // Experimental: this is only required if QuarkusStorageThreadContext pans out
    QuarkusThreadContext copy();

    public interface Factory {
        QuarkusThreadContext newContext();
    }

}

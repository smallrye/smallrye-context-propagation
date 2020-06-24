package io.smallrye.context.storage;

/**
 * Marker interface to be stuck into the QuarkusThread
 */
public interface QuarkusContexts {

    // this is only required if QuarkusStorageThreadContext pans out
    QuarkusContexts copy();

}

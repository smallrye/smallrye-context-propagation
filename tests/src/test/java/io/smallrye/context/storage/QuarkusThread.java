package io.smallrye.context.storage;

/**
 * Base class for all Quarkus threads
 */
interface QuarkusThread {

    Object[] getQuarkusThreadContext();

    // Experimental
    //    void setQuarkusThreadContext(QuarkusThreadContext context);
}
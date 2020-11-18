package io.smallrye.context.storage;

/**
 * Base class for all Quarkus threads
 */
interface QuarkusThread {

    QuarkusThreadContext getQuarkusThreadContext();

    // Experimental
    void setQuarkusThreadContext(QuarkusThreadContext context);
}
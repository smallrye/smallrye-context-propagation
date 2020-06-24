package io.smallrye.context.storage;

/**
 * Base class for all Quarkus thread pools
 */
class QuarkusThread extends Thread {
    // this RHS is generated somehow
    public QuarkusContexts contexts = new QuarkusContextsImpl();

    public QuarkusThread(Runnable r) {
        super(r);
    }
}
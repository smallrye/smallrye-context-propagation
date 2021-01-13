package io.smallrye.context.storage;

public class QuarkusThreadImpl extends Thread implements QuarkusThread {

    private QuarkusThreadContext contexts = QuarkusStorageManager.instance().newContext();

    public QuarkusThreadImpl(Runnable r) {
        super(r);
    }

    @Override
    public QuarkusThreadContext getQuarkusThreadContext() {
        return contexts;
    }

    // Experimental
    @Override
    public void setQuarkusThreadContext(QuarkusThreadContext context) {
        this.contexts = context;
    }

}

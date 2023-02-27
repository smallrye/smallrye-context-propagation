package io.smallrye.context.test;

import io.smallrye.context.storage.spi.StorageDeclaration;
import io.smallrye.context.storage.spi.StorageManager;

public class MyContext {

    static class Declaration implements StorageDeclaration<MyContext> {
    }

    static ThreadLocal<MyContext> context = StorageManager.threadLocal(Declaration.class);

    public static void init() {
        context.set(new MyContext());
    }

    public static void clear() {
        context.remove();
    }

    public static MyContext get() {
        return context.get();
    }

    public static void set(MyContext newContext) {
        context.set(newContext);
    }

    private String reqId;

    public void set(String reqId) {
        this.reqId = reqId;
    }

    public String getReqId() {
        return reqId;
    }
}

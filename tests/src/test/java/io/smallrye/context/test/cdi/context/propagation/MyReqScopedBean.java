package io.smallrye.context.test.cdi.context.propagation;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MyReqScopedBean {

    private AtomicInteger state = new AtomicInteger(0);

    public int incrementState() {
        return state.incrementAndGet();
    }

    public int getState() {
        return state.get();
    }
}

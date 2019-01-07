package io.smallrye.concurrency.test.cdi.context.propagation;

import javax.enterprise.context.RequestScoped;
import java.util.concurrent.atomic.AtomicInteger;

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

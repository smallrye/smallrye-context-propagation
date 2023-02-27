package io.smallrye.context.test.jta;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.transaction.TransactionScoped;

@TransactionScoped
public class TransactionalBeanImpl implements TransactionalBean, Serializable {
    private AtomicInteger value = new AtomicInteger(0);

    public int getValue() {
        return value.get();
    }

    public void incrementValue() {
        value.incrementAndGet();
    }
}

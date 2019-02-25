package io.smallrye.concurrency.test.jta;

import javax.transaction.TransactionScoped;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

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

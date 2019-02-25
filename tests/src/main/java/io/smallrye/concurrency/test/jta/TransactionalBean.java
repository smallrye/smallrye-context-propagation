package io.smallrye.concurrency.test.jta;

public interface TransactionalBean {
    int getValue();
    void incrementValue();
}

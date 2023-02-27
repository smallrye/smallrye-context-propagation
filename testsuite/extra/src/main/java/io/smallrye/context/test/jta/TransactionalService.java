package io.smallrye.context.test.jta;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.context.ManagedExecutor;

@Dependent
public class TransactionalService {
    @Inject
    private TransactionalBean bean;

    public int getValue() {
        return bean.getValue();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void requiresNew() {
        bean.incrementValue();
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public void required() {
        bean.incrementValue();
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void mandatory() {
        bean.incrementValue();
    }

    @Transactional(Transactional.TxType.NEVER)
    public void never() {
        bean.incrementValue(); // should throe ContextNotActiveException
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void notSupported() {
        bean.incrementValue(); // should throe ContextNotActiveException
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public void supports() {
        bean.incrementValue();
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public int testAsync(ManagedExecutor executor) {
        int currentValue = bean.getValue();
        CompletableFuture<Void> stage = executor.runAsync(this::required);
        stage.join();

        return bean.getValue() - currentValue;
    }
}

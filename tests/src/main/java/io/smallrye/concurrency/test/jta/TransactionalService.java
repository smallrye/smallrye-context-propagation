package io.smallrye.concurrency.test.jta;

import org.eclipse.microprofile.context.ManagedExecutor;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.concurrent.CompletableFuture;

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

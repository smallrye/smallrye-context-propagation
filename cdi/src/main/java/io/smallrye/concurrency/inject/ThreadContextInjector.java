package io.smallrye.concurrency.inject;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.ThreadContextConfig;

import io.smallrye.concurrency.SmallRyeConcurrencyManager;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;
import io.smallrye.concurrency.impl.ThreadContextImpl;

@ApplicationScoped
public class ThreadContextInjector {

    @Produces
    public ThreadContext getThreadContext(InjectionPoint injectionPoint) {
        ThreadContextConfig config = injectionPoint.getAnnotated().getAnnotation(ThreadContextConfig.class);
        SmallRyeConcurrencyManager manager = SmallRyeConcurrencyProvider.getManager();
        String[] propagated;
        String[] unchanged;
        String[] cleared;
        if (config != null) {
            propagated = config.value();
            unchanged = config.unchanged();
            cleared = config.cleared();
        } else {
            propagated = SmallRyeConcurrencyManager.ALL_REMAINING_ARRAY;
            unchanged = SmallRyeConcurrencyManager.NO_STRING;
            cleared = SmallRyeConcurrencyManager.TRANSACTION_ARRAY;
        }
        return new ThreadContextImpl(manager, propagated, unchanged, cleared);
    }
}

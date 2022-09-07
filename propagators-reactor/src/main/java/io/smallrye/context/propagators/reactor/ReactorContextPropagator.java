package io.smallrye.context.propagators.reactor;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import reactor.core.scheduler.Schedulers;

public class ReactorContextPropagator implements ContextManagerExtension {

    @Override
    public void setup(ContextManager manager) {

        ThreadContext threadContext = manager.newThreadContextBuilder().build();

        Schedulers.onScheduleHook(
            ReactorContextPropagator.class.getName(),
            threadContext::contextualRunnable);

    }

}

package io.smallrye.context.impl;

import static io.smallrye.context.logging.SmallRyeContextPropagationLogger.ROOT_LOGGER;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.SmallRyeContextManager;

public class CapturedContextState {

    private List<ThreadContextSnapshot> threadContext = new LinkedList<>();
    private SmallRyeContextManager context;

    public CapturedContextState(SmallRyeContextManager context, ThreadContextProviderPlan plan,
            Map<String, String> props) {
        try {
            this.context = context;
            for (ThreadContextProvider provider : plan.propagatedProviders) {
                ThreadContextSnapshot snapshot = provider.currentContext(props);
                if (snapshot != null) {
                    threadContext.add(snapshot);
                }
            }
            for (ThreadContextProvider provider : plan.clearedProviders) {
                ThreadContextSnapshot snapshot = provider.clearedContext(props);
                if (snapshot != null) {
                    threadContext.add(snapshot);
                }
            }
        } catch (Throwable t) {
            ROOT_LOGGER.errorGettingSnapshot(t.getLocalizedMessage());
            t.printStackTrace();
            Util.rethrow(t);
        }
    }

    public ActiveContextState begin() {
        return new ActiveContextState(context, threadContext);
    }
}

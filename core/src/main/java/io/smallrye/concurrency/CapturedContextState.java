package io.smallrye.concurrency;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.concurrency.impl.ThreadContextProviderPlan;

public class CapturedContextState {

    private List<ThreadContextSnapshot> threadContext = new LinkedList<>();
    private SmallRyeConcurrencyManager context;

    CapturedContextState(SmallRyeConcurrencyManager context, ThreadContextProviderPlan plan,
            Map<String, String> props) {
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
    }

    public ActiveContextState begin() {
        return new ActiveContextState(context, threadContext);
    }
}

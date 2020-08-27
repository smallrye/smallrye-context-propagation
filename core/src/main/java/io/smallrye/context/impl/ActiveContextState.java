package io.smallrye.context.impl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.SmallRyeContextManager;

public class ActiveContextState implements AutoCloseable {

    private List<ThreadContextController> activeContext;

    public ActiveContextState(SmallRyeContextManager context, List<ThreadContextSnapshot> threadContext) {
        activeContext = new ArrayList<>(threadContext.size());
        for (ThreadContextSnapshot threadContextSnapshot : threadContext) {
            activeContext.add(threadContextSnapshot.begin());
        }
    }

    public void close() {
        // restore in reverse order
        for (int i = activeContext.size() - 1; i >= 0; i--) {
            activeContext.get(i).endContext();
        }
    }
}

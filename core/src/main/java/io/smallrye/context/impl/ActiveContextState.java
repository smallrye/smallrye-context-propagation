package io.smallrye.context.impl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeThreadContext;

public class ActiveContextState implements AutoCloseable {

    private List<ThreadContextController> activeContext;
    private CleanAutoCloseable activeThreadContext;

    public ActiveContextState(SmallRyeThreadContext threadContext, List<ThreadContextSnapshot> threadContextSnapshots) {
        activeContext = new ArrayList<>(threadContextSnapshots.size());
        for (ThreadContextSnapshot threadContextSnapshot : threadContextSnapshots) {
            activeContext.add(threadContextSnapshot.begin());
        }
        activeThreadContext = SmallRyeThreadContext.withThreadContext(threadContext);
    }

    public void close() {
        // restore in reverse order
        for (int i = activeContext.size() - 1; i >= 0; i--) {
            activeContext.get(i).endContext();
        }
        activeThreadContext.close();
    }
}

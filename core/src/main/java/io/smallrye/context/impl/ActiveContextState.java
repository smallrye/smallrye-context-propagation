package io.smallrye.context.impl;

import java.util.List;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.CleanAutoCloseable;
import io.smallrye.context.SmallRyeThreadContext;

public class ActiveContextState implements AutoCloseable {

    private ThreadContextController[] activeContext;
    private CleanAutoCloseable activeThreadContext;

    public ActiveContextState(SmallRyeThreadContext threadContext, List<ThreadContextSnapshot> threadContextSnapshots) {
        activeContext = new ThreadContextController[threadContextSnapshots.size()];
        int i = 0;
        for (ThreadContextSnapshot threadContextSnapshot : threadContextSnapshots) {
            activeContext[i++] = threadContextSnapshot.begin();
        }
        activeThreadContext = SmallRyeThreadContext.withThreadContext(threadContext);
    }

    public void close() {
        // restore in reverse order
        for (int i = activeContext.length - 1; i >= 0; i--) {
            activeContext[i].endContext();
        }
        activeThreadContext.close();
    }
}

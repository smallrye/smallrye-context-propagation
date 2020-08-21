package io.smallrye.context.impl;

import static io.smallrye.context.logging.SmallRyeContextPropagationLogger.ROOT_LOGGER;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.smallrye.context.SmallRyeContextManager;

public class ActiveContextState {

    private List<ThreadContextController> activeContext;

    public ActiveContextState(SmallRyeContextManager context, List<ThreadContextSnapshot> threadContext) {
        try {
            activeContext = new ArrayList<>(threadContext.size());
            for (ThreadContextSnapshot threadContextSnapshot : threadContext) {
                activeContext.add(threadContextSnapshot.begin());
            }
        } catch (Throwable t) {
            ROOT_LOGGER.errorBeginningThreadContextSnapshot(t.getLocalizedMessage());
            if (ROOT_LOGGER.isDebugEnabled()) {
                t.printStackTrace();
            }
            Util.rethrow(t);
        }
    }

    public void endContext() {
        try {
            // restore in reverse order
            for (int i = activeContext.size() - 1; i >= 0; i--) {
                activeContext.get(i).endContext();
            }
        } catch (Throwable t) {
            ROOT_LOGGER.errorEndingContext(t.getLocalizedMessage());
            if (ROOT_LOGGER.isDebugEnabled()) {
                t.printStackTrace();
            }
            Util.rethrow(t);
        }
    }

}

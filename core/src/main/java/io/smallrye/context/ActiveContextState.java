package io.smallrye.context;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

public class ActiveContextState {

    private List<ThreadContextController> activeContext;
    private SmallRyeContextManager previousContext;

    public ActiveContextState(SmallRyeContextManager context, List<ThreadContextSnapshot> threadContext) {
//		previousContext = SmallRyeConcurrencyProvider.setLocalManager(context);
        activeContext = new ArrayList<>(threadContext.size());
        for (ThreadContextSnapshot threadContextSnapshot : threadContext) {
            activeContext.add(threadContextSnapshot.begin());
        }
    }

    public void endContext() {
//		SmallRyeConcurrencyProvider.setLocalManager(previousContext);
        // restore in reverse order
        for (int i = activeContext.size() - 1; i >= 0; i--) {
            activeContext.get(i).endContext();
        }
    }
}

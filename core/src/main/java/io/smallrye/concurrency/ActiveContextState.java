package io.smallrye.concurrency;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.concurrent.spi.ThreadContextController;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

public class ActiveContextState {

	private List<ThreadContextController> activeContext;
	private SmallRyeConcurrencyManager previousContext;

	public ActiveContextState(SmallRyeConcurrencyManager context, List<ThreadContextSnapshot> threadContext) {
		previousContext = SmallRyeConcurrencyProvider.setLocalManager(context);
		activeContext = new ArrayList<>(threadContext.size());
		for (ThreadContextSnapshot threadContextSnapshot : threadContext) {
			activeContext.add(threadContextSnapshot.begin());
		}
	}

	public void endContext() {
		SmallRyeConcurrencyProvider.setLocalManager(previousContext);
		for (ThreadContextController activeThreadContext : activeContext) {
			activeThreadContext.endContext();
		}
	}
}

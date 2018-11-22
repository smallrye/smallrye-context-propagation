package io.smallrye.concurrency;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

import io.smallrye.concurrency.impl.ThreadContextProviderPlan;

public class CapturedContextState {

	private List<ThreadContextSnapshot> threadContext = new LinkedList<>();
	private SmallRyeConcurrencyManager context;
	
	CapturedContextState(SmallRyeConcurrencyManager context, ThreadContextProviderPlan plan, Map<String, String> props){
		this.context = context;
		for (ThreadContextProvider provider : plan.propagatedProviders) {
			threadContext.add(provider.currentContext(props));
		}
		for (ThreadContextProvider provider : plan.clearedProviders) {
			threadContext.add(provider.clearedContext(props));
		}
	}
	
	public ActiveContextState begin() {
		return new ActiveContextState(context, threadContext);
	}
}

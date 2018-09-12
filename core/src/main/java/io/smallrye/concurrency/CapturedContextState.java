package io.smallrye.concurrency;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

public class CapturedContextState {

	private List<ThreadContextSnapshot> threadContext = new LinkedList<>();
	private SmallRyeConcurrencyManager context;
	
	CapturedContextState(SmallRyeConcurrencyManager context, List<ThreadContextProvider> providers, Map<String, String> props){
		this.context = context;
		for (ThreadContextProvider provider : providers) {
			threadContext.add(provider.currentContext(props));
		}
	}
	
	public ActiveContextState begin() {
		return new ActiveContextState(context, threadContext);
	}
}

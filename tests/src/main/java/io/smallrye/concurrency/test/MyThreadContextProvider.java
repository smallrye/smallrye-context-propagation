package io.smallrye.concurrency.test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

public class MyThreadContextProvider implements ThreadContextProvider {

	@Override
	public ThreadContextSnapshot currentContext(Map<String, String> props) {
		MyContext capturedContext = MyContext.get();
		return () -> {
			MyContext movedContext = MyContext.get();
			MyContext.set(capturedContext);
			return () -> {
				MyContext.set(movedContext);
			};
		};
	}

	@Override
	public ThreadContextSnapshot defaultContext(Map<String, String> props) {
		return () -> {
			MyContext movedContext = MyContext.get();
			MyContext.clear();
			return () -> {
				if(movedContext == null)
					MyContext.clear();
				else
					MyContext.set(movedContext);
			};
		};
	}

	@Override
	public Set<String> getPrerequisites() {
		return Collections.emptySet();
	}

	@Override
	public String getThreadContextType() {
		return "MyContext";
	}

}

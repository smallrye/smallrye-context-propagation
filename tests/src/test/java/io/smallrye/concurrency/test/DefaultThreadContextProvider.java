package io.smallrye.concurrency.test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

public class DefaultThreadContextProvider implements ThreadContextProvider {

	private String type;
	private List<String> record;

	public DefaultThreadContextProvider(String type, List<String> record) {
		this.type = type;
		this.record = record;
	}

	@Override
	public ThreadContextSnapshot currentContext(Map<String, String> props) {
		return () -> {
			record.add("current before: "+type);
			return () -> {
				record.add("current after: "+type);
			};
		};
	}

	@Override
	public ThreadContextSnapshot defaultContext(Map<String, String> props) {
		return () -> {
			record.add("default before: "+type);
			return () -> {
				record.add("default after: "+type);
			};
		};
	}

	@Override
	public Set<String> getPrerequisites() {
		return Collections.emptySet();
	}

	@Override
	public String getThreadContextType() {
		return type;
	}
}

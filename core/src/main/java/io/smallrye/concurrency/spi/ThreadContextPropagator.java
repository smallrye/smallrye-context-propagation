package io.smallrye.concurrency.spi;

import org.eclipse.microprofile.concurrent.ThreadContext;

public interface ThreadContextPropagator {
	public void setup(ThreadContext threadContext);
}

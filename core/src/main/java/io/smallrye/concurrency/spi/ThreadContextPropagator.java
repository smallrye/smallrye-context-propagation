package io.smallrye.concurrency.spi;

import org.eclipse.microprofile.concurrent.spi.ConcurrencyManager;

public interface ThreadContextPropagator {
	public void setup(ConcurrencyManager manager);
}

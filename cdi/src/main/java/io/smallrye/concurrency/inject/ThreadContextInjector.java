package io.smallrye.concurrency.inject;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.ThreadContextConfig;

import io.smallrye.concurrency.SmallRyeConcurrencyManager;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;
import io.smallrye.concurrency.impl.ThreadContextImpl;

@ApplicationScoped
public class ThreadContextInjector {

	// WHY THIS NO WORK?!?!?!?!?! (╯°□°）╯︵ ┻━┻
//	@Produces
//	public ThreadContext getThreadContext(InjectionPoint injectionPoint) {
//		ThreadContextConfig config = injectionPoint.getAnnotated().getAnnotation(ThreadContextConfig.class);
//		SmallRyeConcurrencyManager manager = SmallRyeConcurrencyProvider.getManager();
//		String[] propagated;
//		String[] unchanged;
//		if(config != null) {
//			propagated = config.value();
//			unchanged = config.unchanged();
//		}else {
//			propagated = manager.getAllProviderTypes();
//			unchanged = SmallRyeConcurrencyManager.NO_STRING;
//		}
//		return new ThreadContextImpl(manager, propagated, unchanged);
//	}
}

package io.smallrye.concurrency.propagators.rxjava1;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyManager;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyManagerExtension;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.plugins.RxJavaHooks;

/**
 * Reactive Context propagator for RxJava 1. Supports propagating context to all {@link Single},
 * {@link Observable} and {@link Completable} types.
 *
 * @author Stéphane Épardaud
 */
public class RxJava1ContextPropagator implements ConcurrencyManagerExtension {

	public void setup(ConcurrencyManager manager) {
		ThreadContext threadContext = manager.newThreadContextBuilder().build();
		RxJavaHooks.setOnSingleCreate(new ContextPropagatorOnSingleCreateAction(threadContext));
		RxJavaHooks.setOnObservableCreate(new ContextPropagatorOnObservableCreateAction(threadContext));
		RxJavaHooks.setOnCompletableCreate(new ContextPropagatorOnCompleteCreateAction(threadContext));
	}

}

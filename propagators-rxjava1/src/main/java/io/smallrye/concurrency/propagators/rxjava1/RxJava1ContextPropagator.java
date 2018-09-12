package io.smallrye.concurrency.propagators.rxjava1;

import io.smallrye.concurrency.spi.ThreadContextPropagator;
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
public class RxJava1ContextPropagator implements ThreadContextPropagator {

	public void setup() {
		RxJavaHooks.setOnSingleCreate(new ContextPropagatorOnSingleCreateAction());
		RxJavaHooks.setOnObservableCreate(new ContextPropagatorOnObservableCreateAction());
		RxJavaHooks.setOnCompletableCreate(new ContextPropagatorOnCompleteCreateAction());
	}

}

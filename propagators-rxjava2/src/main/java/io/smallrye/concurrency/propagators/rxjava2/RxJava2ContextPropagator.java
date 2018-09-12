package io.smallrye.concurrency.propagators.rxjava2;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.plugins.RxJavaPlugins;
import io.smallrye.concurrency.spi.ThreadContextPropagator;

/**
 * Reactive Context propagator for RxJava 1. Supports propagating context to all {@link Single},
 * {@link Observable}, {@link Completable}, {@link Flowable} and {@link Maybe} types.
 *
 * @author Stéphane Épardaud
 */
public class RxJava2ContextPropagator implements ThreadContextPropagator {

	public void setup() {
		RxJavaPlugins.setOnSingleSubscribe(new ContextPropagatorOnSingleCreateAction());
		RxJavaPlugins.setOnCompletableSubscribe(new ContextPropagatorOnCompletableCreateAction());
		RxJavaPlugins.setOnFlowableSubscribe(new ContextPropagatorOnFlowableCreateAction());
		RxJavaPlugins.setOnMaybeSubscribe(new ContextPropagatorOnMaybeCreateAction());
		RxJavaPlugins.setOnObservableSubscribe(new ContextPropagatorOnObservableCreateAction());
		
		RxJavaPlugins.setOnSingleAssembly(new ContextPropagatorOnSingleAssemblyAction());
		RxJavaPlugins.setOnCompletableAssembly(new ContextPropagatorOnCompletableAssemblyAction());
		RxJavaPlugins.setOnFlowableAssembly(new ContextPropagatorOnFlowableAssemblyAction());
		RxJavaPlugins.setOnMaybeAssembly(new ContextPropagatorOnMaybeAssemblyAction());
		RxJavaPlugins.setOnObservableAssembly(new ContextPropagatorOnObservableAssemblyAction());
	}

}

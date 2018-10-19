package io.smallrye.concurrency.propagators.rxjava2;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyManager;

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

	public void setup(ConcurrencyManager manager) {
		ThreadContext threadContext = manager.newThreadContextBuilder().propagated(ThreadContext.ALL).build();
		RxJavaPlugins.setOnSingleSubscribe(new ContextPropagatorOnSingleCreateAction(threadContext));
		RxJavaPlugins.setOnCompletableSubscribe(new ContextPropagatorOnCompletableCreateAction(threadContext));
		RxJavaPlugins.setOnFlowableSubscribe(new ContextPropagatorOnFlowableCreateAction(threadContext));
		RxJavaPlugins.setOnMaybeSubscribe(new ContextPropagatorOnMaybeCreateAction(threadContext));
		RxJavaPlugins.setOnObservableSubscribe(new ContextPropagatorOnObservableCreateAction(threadContext));
		
		RxJavaPlugins.setOnSingleAssembly(new ContextPropagatorOnSingleAssemblyAction(threadContext));
		RxJavaPlugins.setOnCompletableAssembly(new ContextPropagatorOnCompletableAssemblyAction(threadContext));
		RxJavaPlugins.setOnFlowableAssembly(new ContextPropagatorOnFlowableAssemblyAction(threadContext));
		RxJavaPlugins.setOnMaybeAssembly(new ContextPropagatorOnMaybeAssemblyAction(threadContext));
		RxJavaPlugins.setOnObservableAssembly(new ContextPropagatorOnObservableAssemblyAction(threadContext));
	}

}

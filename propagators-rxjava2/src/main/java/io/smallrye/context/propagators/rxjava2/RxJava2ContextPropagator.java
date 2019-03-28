package io.smallrye.context.propagators.rxjava2;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerExtension;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Reactive Context propagator for RxJava 1. Supports propagating context to all
 * {@link Single}, {@link Observable}, {@link Completable}, {@link Flowable} and
 * {@link Maybe} types.
 *
 * @author Stéphane Épardaud
 */
public class RxJava2ContextPropagator implements ContextManagerExtension {

    public void setup(ContextManager manager) {
        ThreadContext threadContext = manager.newThreadContextBuilder().build();
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

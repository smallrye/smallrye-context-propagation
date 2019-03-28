package io.smallrye.concurrency.propagators.rxjava1;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerExtension;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.plugins.RxJavaHooks;

/**
 * Reactive Context propagator for RxJava 1. Supports propagating context to all
 * {@link Single}, {@link Observable} and {@link Completable} types.
 *
 * @author Stéphane Épardaud
 */
public class RxJava1ContextPropagator implements ContextManagerExtension {

    public void setup(ContextManager manager) {
        ThreadContext threadContext = manager.newThreadContextBuilder().build();
        RxJavaHooks.setOnSingleCreate(new ContextPropagatorOnSingleCreateAction(threadContext));
        RxJavaHooks.setOnObservableCreate(new ContextPropagatorOnObservableCreateAction(threadContext));
        RxJavaHooks.setOnCompletableCreate(new ContextPropagatorOnCompleteCreateAction(threadContext));
    }

}

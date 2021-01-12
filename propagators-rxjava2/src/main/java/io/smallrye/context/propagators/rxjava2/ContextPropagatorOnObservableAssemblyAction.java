package io.smallrye.context.propagators.rxjava2;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ThreadContext;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.functions.Function;

@SuppressWarnings("rawtypes")
public class ContextPropagatorOnObservableAssemblyAction implements Function<Observable, Observable> {

    private ThreadContext threadContext;

    public ContextPropagatorOnObservableAssemblyAction(ThreadContext threadContext) {
        this.threadContext = threadContext;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable apply(Observable t) throws Exception {
        return new ContextPropagatorObservable(t, threadContext.currentContextExecutor());
    }

    public static class ContextPropagatorObservable<T> extends Observable<T> {

        private final Observable<T> source;

        private final Executor contextExecutor;

        public ContextPropagatorObservable(Observable<T> t, Executor contextExecutor) {
            this.source = t;
            this.contextExecutor = contextExecutor;
        }

        @Override
        protected void subscribeActual(Observer<? super T> observer) {
            contextExecutor.execute(() -> source.subscribe(observer));
        }

    }

}

package io.smallrye.context.propagators.rxjava2;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ThreadContext;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.functions.Function;

public class ContextPropagatorOnCompletableAssemblyAction implements Function<Completable, Completable> {

    private ThreadContext threadContext;

    public ContextPropagatorOnCompletableAssemblyAction(ThreadContext threadContext) {
        this.threadContext = threadContext;
    }

    @Override
    public Completable apply(Completable t) throws Exception {
        return new ContextPropagatorCompletable(t, threadContext.currentContextExecutor());
    }

    public static class ContextPropagatorCompletable extends Completable {

        private final Completable source;

        private final Executor contextExecutor;

        public ContextPropagatorCompletable(Completable t, Executor contextExecutor) {
            this.source = t;
            this.contextExecutor = contextExecutor;
        }

        @Override
        protected void subscribeActual(CompletableObserver observer) {
            contextExecutor.execute(() -> source.subscribe(observer));
        }

    }

}

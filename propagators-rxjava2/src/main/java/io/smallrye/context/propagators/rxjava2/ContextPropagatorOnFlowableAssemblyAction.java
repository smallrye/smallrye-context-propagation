package io.smallrye.context.propagators.rxjava2;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ThreadContext;
import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;

@SuppressWarnings("rawtypes")
public class ContextPropagatorOnFlowableAssemblyAction implements Function<Flowable, Flowable> {

    private ThreadContext threadContext;

    public ContextPropagatorOnFlowableAssemblyAction(ThreadContext threadContext) {
        this.threadContext = threadContext;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Flowable apply(Flowable t) throws Exception {
        return new ContextPropagatorFlowable(t, threadContext.currentContextExecutor());
    }

    public static class ContextPropagatorFlowable<T> extends Flowable<T> {

        private final Flowable<T> source;

        private final Executor contextExecutor;

        public ContextPropagatorFlowable(Flowable<T> t, Executor contextExecutor) {
            this.source = t;
            this.contextExecutor = contextExecutor;
        }

        @Override
        protected void subscribeActual(Subscriber<? super T> observer) {
            contextExecutor.execute(() -> source.subscribe(observer));
        }

    }

}

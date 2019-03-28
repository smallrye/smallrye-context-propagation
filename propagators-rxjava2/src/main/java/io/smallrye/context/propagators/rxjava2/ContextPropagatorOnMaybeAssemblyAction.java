package io.smallrye.context.propagators.rxjava2;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ThreadContext;

import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.functions.Function;

@SuppressWarnings("rawtypes")
public class ContextPropagatorOnMaybeAssemblyAction implements Function<Maybe, Maybe> {

    private ThreadContext threadContext;

    public ContextPropagatorOnMaybeAssemblyAction(ThreadContext threadContext) {
        this.threadContext = threadContext;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Maybe apply(Maybe t) throws Exception {
        return new ContextPropagatorMaybe(t, threadContext.currentContextExecutor());
    }

    public class ContextPropagatorMaybe<T> extends Maybe<T> {

        private Maybe<T> source;

        private final Executor contextExecutor;

        public ContextPropagatorMaybe(Maybe<T> t, Executor contextExecutor) {
            this.source = t;
            this.contextExecutor = contextExecutor;
        }

        @Override
        protected void subscribeActual(MaybeObserver<? super T> observer) {
            contextExecutor.execute(() -> source.subscribe(observer));
        }

    }

}

package io.smallrye.context.propagators.rxjava2;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ThreadContext;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;

@SuppressWarnings("rawtypes")
public class ContextPropagatorOnSingleCreateAction implements BiFunction<Single, SingleObserver, SingleObserver> {

    private ThreadContext threadContext;

    public ContextPropagatorOnSingleCreateAction(ThreadContext threadContext) {
        this.threadContext = threadContext;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SingleObserver apply(Single s, SingleObserver o) throws Exception {
        return new ContextCapturerSingle(s, o, threadContext.currentContextExecutor());
    }

    final static class ContextCapturerSingle<T> implements SingleObserver<T> {

        private final SingleObserver<T> source;

        private final Executor contextExecutor;

        public ContextCapturerSingle(Single<T> s, SingleObserver<T> o, Executor contextExecutor) {
            this.source = o;
            this.contextExecutor = contextExecutor;
        }

        @Override
        public void onError(Throwable t) {
            contextExecutor.execute(() -> source.onError(t));
        }

        @Override
        public void onSubscribe(Disposable d) {
            contextExecutor.execute(() -> source.onSubscribe(d));
        }

        @Override
        public void onSuccess(T v) {
            contextExecutor.execute(() -> source.onSuccess(v));
        }
    }
}

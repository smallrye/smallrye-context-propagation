package io.smallrye.concurrency.propagators.rxjava2;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ThreadContext;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;

public class ContextPropagatorOnCompletableCreateAction implements BiFunction<Completable, CompletableObserver, CompletableObserver> {

    private ThreadContext threadContext;

    public ContextPropagatorOnCompletableCreateAction(ThreadContext threadContext) {
        this.threadContext = threadContext;
    }

    @Override
    public CompletableObserver apply(Completable completable, CompletableObserver observer) throws Exception {
        return new ContextCapturerCompletable(completable, observer, threadContext.currentContextExecutor());
    }

    final static class ContextCapturerCompletable implements CompletableObserver {

        private final CompletableObserver source;

        private final Executor contextExecutor;

        public ContextCapturerCompletable(Completable s, CompletableObserver o, Executor contextExecutor) {
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
        public void onComplete() {
            contextExecutor.execute(() -> source.onComplete());
        }
    }

}

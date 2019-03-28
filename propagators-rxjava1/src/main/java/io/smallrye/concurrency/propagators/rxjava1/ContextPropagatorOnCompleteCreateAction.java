package io.smallrye.concurrency.propagators.rxjava1;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ThreadContext;

import rx.Completable.OnSubscribe;
import rx.CompletableSubscriber;
import rx.Subscription;
import rx.functions.Func1;

public class ContextPropagatorOnCompleteCreateAction implements Func1<OnSubscribe, OnSubscribe> {

    private ThreadContext threadContext;

    public ContextPropagatorOnCompleteCreateAction(ThreadContext threadContext) {
        this.threadContext = threadContext;
    }

    @Override
    public OnSubscribe call(OnSubscribe t) {
        return new ContextCapturerCompletable(t, threadContext.currentContextExecutor());
    }

    final static class ContextCapturerCompletable implements OnSubscribe {

        final OnSubscribe source;

        private Executor contextExecutor;

        public ContextCapturerCompletable(OnSubscribe source, Executor contextExecutor) {
            this.source = source;
            this.contextExecutor = contextExecutor;
        }

        @Override
        public void call(CompletableSubscriber t) {
            contextExecutor.execute(() -> source.call(new OnAssemblyCompletableSubscriber(t, contextExecutor)));
        }

        static final class OnAssemblyCompletableSubscriber implements CompletableSubscriber {

            final CompletableSubscriber actual;

            private Executor contextExecutor;

            public OnAssemblyCompletableSubscriber(CompletableSubscriber actual, Executor contextExecutor) {
                this.actual = actual;
                this.contextExecutor = contextExecutor;
            }

            @Override
            public void onError(Throwable e) {
                contextExecutor.execute(() -> actual.onError(e));
            }

            @Override
            public void onCompleted() {
                contextExecutor.execute(() -> actual.onCompleted());
            }

            @Override
            public void onSubscribe(Subscription d) {
                contextExecutor.execute(() -> actual.onSubscribe(d));
            }
        }
    }

}

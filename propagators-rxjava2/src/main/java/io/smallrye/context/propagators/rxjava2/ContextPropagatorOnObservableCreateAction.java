package io.smallrye.context.propagators.rxjava2;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.context.ThreadContext;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;

@SuppressWarnings("rawtypes")
public class ContextPropagatorOnObservableCreateAction implements BiFunction<Observable, Observer, Observer> {

    private ThreadContext threadContext;

    public ContextPropagatorOnObservableCreateAction(ThreadContext threadContext) {
        this.threadContext = threadContext;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observer apply(Observable observable, Observer observer) throws Exception {
        return new ContextCapturerObservable(observable, observer, threadContext.currentContextExecutor());
    }

    public static class ContextCapturerObservable<T> implements Observer<T> {

        private final Observer<T> source;

        private final Executor contextExecutor;

        public ContextCapturerObservable(Observable<T> observable, Observer<T> observer, Executor contextExecutor) {
            this.source = observer;
            this.contextExecutor = contextExecutor;
        }

        @Override
        public void onComplete() {
            contextExecutor.execute(() -> source.onComplete());
        }

        @Override
        public void onError(Throwable t) {
            contextExecutor.execute(() -> source.onError(t));
        }

        @Override
        public void onNext(T v) {
            contextExecutor.execute(() -> source.onNext(v));
        }

        @Override
        public void onSubscribe(Disposable d) {
            contextExecutor.execute(() -> source.onSubscribe(d));
        }
    }
}

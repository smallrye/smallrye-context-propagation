package io.smallrye.concurrency.propagators.rxjava1;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.concurrent.ThreadContext;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;

@SuppressWarnings("rawtypes")
public class ContextPropagatorOnObservableCreateAction implements Func1<OnSubscribe, OnSubscribe> {

	private ThreadContext threadContext;

	public ContextPropagatorOnObservableCreateAction(ThreadContext threadContext) {
		this.threadContext = threadContext;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public OnSubscribe call(OnSubscribe t) {
		return new ContextCapturerObservable(t, threadContext.withCurrentContext());
	}
	
	final static class ContextCapturerObservable<T> implements Observable.OnSubscribe<T> {

	    final Observable.OnSubscribe<T> source;

		private Executor contextExecutor;

	    public ContextCapturerObservable(Observable.OnSubscribe<T> source, Executor contextExecutor) {
	        this.source = source;
	        this.contextExecutor = contextExecutor;
	    }

		@Override
		public void call(Subscriber<? super T> t) {
			contextExecutor.execute(() -> source.call(new OnAssemblyObservableSubscriber<T>(t, contextExecutor)));
		}

	    static final class OnAssemblyObservableSubscriber<T> extends Subscriber<T> {

	        final Subscriber<? super T> actual;
			private final Executor contextExecutor;


	        public OnAssemblyObservableSubscriber(Subscriber<? super T> actual, Executor contextExecutor) {
	            this.actual = actual;
	            this.contextExecutor = contextExecutor;
	            actual.add(this);
	        }

	        @Override
	        public void onError(Throwable e) {
	        	contextExecutor.execute(() -> actual.onError(e));
	        }

	        @Override
	        public void onNext(T t) {
	        	contextExecutor.execute(() -> actual.onNext(t));
	        }

	        @Override
	        public void onCompleted() {
	        	contextExecutor.execute(() -> actual.onCompleted());
	        }
	    }
	}

}

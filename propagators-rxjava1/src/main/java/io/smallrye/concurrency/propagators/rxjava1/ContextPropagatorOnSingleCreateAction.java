package io.smallrye.concurrency.propagators.rxjava1;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.concurrent.ThreadContext;

import rx.Single;
import rx.Single.OnSubscribe;
import rx.SingleSubscriber;
import rx.functions.Func1;

@SuppressWarnings("rawtypes")
public class ContextPropagatorOnSingleCreateAction implements Func1<OnSubscribe, OnSubscribe> {

	private ThreadContext threadContext;

	public ContextPropagatorOnSingleCreateAction(ThreadContext threadContext) {
		this.threadContext = threadContext;
	}

	@SuppressWarnings("unchecked")
	@Override
	public OnSubscribe call(OnSubscribe t) {
		return new ContextCapturerSingle(t, threadContext.currentContextExecutor());
	}
	
	final static class ContextCapturerSingle<T> implements Single.OnSubscribe<T> {

	    final Single.OnSubscribe<T> source;

		private Executor contextExecutor;

	    public ContextCapturerSingle(Single.OnSubscribe<T> source, Executor contextExecutor) {
	        this.source = source;
	        this.contextExecutor = contextExecutor;
	    }

	    @Override
	    public void call(SingleSubscriber<? super T> t) {
	    	contextExecutor.execute(() -> source.call(new OnAssemblySingleSubscriber<T>(t, contextExecutor)));
	    }

	    static final class OnAssemblySingleSubscriber<T> extends SingleSubscriber<T> {

	        final SingleSubscriber<? super T> actual;
			private final Executor contextExecutor;


	        public OnAssemblySingleSubscriber(SingleSubscriber<? super T> actual, Executor contextExecutor) {
	            this.actual = actual;
		        this.contextExecutor = contextExecutor;
	            actual.add(this);
	        }

	        @Override
	        public void onError(Throwable e) {
	        	contextExecutor.execute(() -> actual.onError(e));
	        }

	        @Override
	        public void onSuccess(T t) {
	        	contextExecutor.execute(() -> actual.onSuccess(t));
	        }
	    }
	}

}

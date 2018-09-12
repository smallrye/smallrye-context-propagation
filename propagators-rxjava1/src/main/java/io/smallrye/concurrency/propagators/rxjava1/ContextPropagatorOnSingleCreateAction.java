package io.smallrye.concurrency.propagators.rxjava1;

import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;
import rx.Single;
import rx.Single.OnSubscribe;
import rx.SingleSubscriber;
import rx.functions.Func1;

public class ContextPropagatorOnSingleCreateAction implements Func1<OnSubscribe, OnSubscribe> {

	@Override
	public OnSubscribe call(OnSubscribe t) {
		return new ContextCapturerSingle(t);
	}
	
	final static class ContextCapturerSingle<T> implements Single.OnSubscribe<T> {

	    final Single.OnSubscribe<T> source;

		private CapturedContextState capturedContext;

	    public ContextCapturerSingle(Single.OnSubscribe<T> source) {
	        this.source = source;
	        capturedContext = SmallRyeConcurrencyProvider.captureContext();
	    }

	    @Override
	    public void call(SingleSubscriber<? super T> t) {
	    	ActiveContextState activeContext = capturedContext.begin();
			try {
	    		source.call(new OnAssemblySingleSubscriber<T>(t, capturedContext));
			}finally {
				activeContext.endContext();
			}
	    }

	    static final class OnAssemblySingleSubscriber<T> extends SingleSubscriber<T> {

	        final SingleSubscriber<? super T> actual;
			private final CapturedContextState capturedContext;


	        public OnAssemblySingleSubscriber(SingleSubscriber<? super T> actual, CapturedContextState capturedContext) {
	            this.actual = actual;
	            this.capturedContext = capturedContext;
	            actual.add(this);
	        }

	        @Override
	        public void onError(Throwable e) {
	        	ActiveContextState activeContext = capturedContext.begin();
				try {
					actual.onError(e);
				}finally {
					activeContext.endContext();
				}
	        }

	        @Override
	        public void onSuccess(T t) {
	        	ActiveContextState activeContext = capturedContext.begin();
				try {
					actual.onSuccess(t);
				}finally {
					activeContext.endContext();
				}
	        }
	    }
	}

}

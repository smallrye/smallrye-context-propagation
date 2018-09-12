package io.smallrye.concurrency.propagators.rxjava2;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;

public class ContextPropagatorOnSingleCreateAction implements BiFunction<Single, SingleObserver, SingleObserver> {

	@Override
	public SingleObserver apply(Single s, SingleObserver o) throws Exception {
		return new ContextCapturerSingle(s, o);
	}

	final static class ContextCapturerSingle<T> implements SingleObserver<T> {

	    private final SingleObserver<T> source;
		private final CapturedContextState capturedContext;

	    public ContextCapturerSingle(Single<T> s, SingleObserver<T> o) {
	    	this.source = o;
	        this.capturedContext = SmallRyeConcurrencyProvider.captureContext();
		}

		@Override
		public void onError(Throwable t) {
        	ActiveContextState activeContext = capturedContext.begin();
			try {
	    		source.onError(t);
			}finally {
				activeContext.endContext();
			}
		}

		@Override
		public void onSubscribe(Disposable d) {
        	ActiveContextState activeContext = capturedContext.begin();
			try {
	    		source.onSubscribe(d);
			}finally {
				activeContext.endContext();
			}
		}

		@Override
		public void onSuccess(T v) {
        	ActiveContextState activeContext = capturedContext.begin();
			try {
	    		source.onSuccess(v);
			}finally {
				activeContext.endContext();
			}
		}
	}
}

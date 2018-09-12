package io.smallrye.concurrency.propagators.rxjava2;

import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;

public class ContextPropagatorOnMaybeCreateAction
		implements BiFunction<Maybe, MaybeObserver, MaybeObserver> {

	@Override
	public MaybeObserver apply(Maybe maybe, MaybeObserver observer) throws Exception {
		return new ContextCapturerMaybe<>(maybe, observer);
	}

	public class ContextCapturerMaybe<T> implements MaybeObserver<T> {

	    private final MaybeObserver<T> source;
		private final CapturedContextState capturedContext;

		public ContextCapturerMaybe(Maybe<T> observable, MaybeObserver<T> observer) {
	    	this.source = observer;
	        this.capturedContext = SmallRyeConcurrencyProvider.captureContext();
		}

		@Override
		public void onComplete() {
        	ActiveContextState activeContext = capturedContext.begin();
			try {
	    		source.onComplete();
			}finally {
				activeContext.endContext();
			}
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

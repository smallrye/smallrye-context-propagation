package io.smallrye.concurrency.propagators.rxjava2;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;

public class ContextPropagatorOnCompletableCreateAction
		implements BiFunction<Completable, CompletableObserver, CompletableObserver> {

	@Override
	public CompletableObserver apply(Completable completable, CompletableObserver observer) throws Exception {
		return new ContextCapturerCompletable(completable, observer);
	}

	final static class ContextCapturerCompletable implements CompletableObserver {

	    private final CompletableObserver source;
		private final CapturedContextState capturedContext;

	    public ContextCapturerCompletable(Completable s, CompletableObserver o) {
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
		public void onComplete() {
        	ActiveContextState activeContext = capturedContext.begin();
			try {
	    		source.onComplete();
			}finally {
				activeContext.endContext();
			}
		}
	}

}

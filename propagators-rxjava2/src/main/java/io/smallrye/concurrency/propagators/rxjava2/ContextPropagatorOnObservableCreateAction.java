package io.smallrye.concurrency.propagators.rxjava2;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;

public class ContextPropagatorOnObservableCreateAction
		implements BiFunction<Observable, Observer, Observer> {

	@Override
	public Observer apply(Observable observable, Observer observer) throws Exception {
		return new ContextCapturerObservable(observable, observer);
	}

	public class ContextCapturerObservable<T> implements Observer<T> {

	    private final Observer<T> source;
		private final CapturedContextState capturedContext;

		public ContextCapturerObservable(Observable<T> observable, Observer<T> observer) {
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
		public void onNext(T v) {
        	ActiveContextState activeContext = capturedContext.begin();
			try {
	    		source.onNext(v);
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
	}
}

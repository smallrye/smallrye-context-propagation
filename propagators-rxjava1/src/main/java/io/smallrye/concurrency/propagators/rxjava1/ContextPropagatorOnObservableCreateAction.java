package io.smallrye.concurrency.propagators.rxjava1;

import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;

public class ContextPropagatorOnObservableCreateAction implements Func1<OnSubscribe, OnSubscribe> {

	@Override
	public OnSubscribe call(OnSubscribe t) {
		return new ContextCapturerObservable(t);
	}
	
	final static class ContextCapturerObservable<T> implements Observable.OnSubscribe<T> {

	    final Observable.OnSubscribe<T> source;

		private CapturedContextState capturedContext;

	    public ContextCapturerObservable(Observable.OnSubscribe<T> source) {
	        this.source = source;
	        capturedContext = SmallRyeConcurrencyProvider.captureContext();
	    }

		@Override
		public void call(Subscriber<? super T> t) {
			ActiveContextState activeContext = capturedContext.begin();
			try {
	    		source.call(new OnAssemblyObservableSubscriber<T>(t, capturedContext));
			}finally {
				activeContext.endContext();
			}
			
		}

	    static final class OnAssemblyObservableSubscriber<T> extends Subscriber<T> {

	        final Subscriber<? super T> actual;
			private final CapturedContextState capturedContext;


	        public OnAssemblyObservableSubscriber(Subscriber<? super T> actual, CapturedContextState capturedContext) {
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
	        public void onNext(T t) {
	        	ActiveContextState activeContext = capturedContext.begin();
				try {
					actual.onNext(t);
				}finally {
					activeContext.endContext();
				}
	        }

	        @Override
	        public void onCompleted() {
	        	ActiveContextState activeContext = capturedContext.begin();
				try {
					actual.onCompleted();
				}finally {
					activeContext.endContext();
				}
	        }
}
	}

}

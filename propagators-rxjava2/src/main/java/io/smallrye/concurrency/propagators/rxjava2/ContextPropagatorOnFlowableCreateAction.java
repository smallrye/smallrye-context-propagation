package io.smallrye.concurrency.propagators.rxjava2;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;
import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;

public class ContextPropagatorOnFlowableCreateAction
		implements BiFunction<Flowable, Subscriber, Subscriber> {

	@Override
	public Subscriber apply(Flowable flowable, Subscriber observer) throws Exception {
		return new ContextCapturerFlowable<>(flowable, observer);
	}

	public class ContextCapturerFlowable<T> implements Subscriber<T> {

	    private final Subscriber<T> source;
		private final CapturedContextState capturedContext;

		public ContextCapturerFlowable(Flowable<T> observable, Subscriber<T> observer) {
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
		public void onSubscribe(Subscription s) {
        	ActiveContextState activeContext = capturedContext.begin();
			try {
	    		source.onSubscribe(s);
			}finally {
				activeContext.endContext();
			}
		}
	}

}

package io.smallrye.concurrency.propagators.rxjava1;

import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;
import rx.Completable.OnSubscribe;
import rx.CompletableSubscriber;
import rx.Subscription;
import rx.functions.Func1;

public class ContextPropagatorOnCompleteCreateAction implements Func1<OnSubscribe, OnSubscribe> {

	@Override
	public OnSubscribe call(OnSubscribe t) {
		return new ContextCapturerCompletable(t);
	}

	final static class ContextCapturerCompletable implements OnSubscribe {

	    final OnSubscribe source;

		private CapturedContextState capturedContext;

	    public ContextCapturerCompletable(OnSubscribe source) {
	        this.source = source;
	        capturedContext = SmallRyeConcurrencyProvider.captureContext();
	    }

	    @Override
	    public void call(CompletableSubscriber t) {
        	ActiveContextState activeContext = capturedContext.begin();
			try {
	    		source.call(new OnAssemblyCompletableSubscriber(t, capturedContext));
			}finally {
				activeContext.endContext();
			}
	    }

	    static final class OnAssemblyCompletableSubscriber implements CompletableSubscriber {

	        final CompletableSubscriber actual;
			private final CapturedContextState capturedContext;


	        public OnAssemblyCompletableSubscriber(CompletableSubscriber actual, CapturedContextState capturedContext) {
	            this.actual = actual;
	            this.capturedContext = capturedContext;
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
	        public void onCompleted() {
	        	ActiveContextState activeContext = capturedContext.begin();
				try {
					actual.onCompleted();
				}finally {
					activeContext.endContext();
				}
	        }

			@Override
			public void onSubscribe(Subscription d) {
				ActiveContextState activeContext = capturedContext.begin();
				try {
					actual.onSubscribe(d);
				}finally {
					activeContext.endContext();
				}
			}
	    }
	}

}

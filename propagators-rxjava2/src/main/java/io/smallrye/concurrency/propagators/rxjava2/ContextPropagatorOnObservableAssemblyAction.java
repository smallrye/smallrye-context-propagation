package io.smallrye.concurrency.propagators.rxjava2;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.functions.Function;
import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;

public class ContextPropagatorOnObservableAssemblyAction implements Function<Observable, Observable> {

	@Override
	public Observable apply(Observable t) throws Exception {
		return new ContextPropagatorObservable(t);
	}

	public class ContextPropagatorObservable<T> extends Observable<T> {

		private Observable<T> source;
		private CapturedContextState capturedContext;

		public ContextPropagatorObservable(Observable<T> t) {
			this.source = t;
			this.capturedContext = SmallRyeConcurrencyProvider.captureContext();
		}

		@Override
		protected void subscribeActual(Observer<? super T> observer) {
			ActiveContextState activeContext = capturedContext.begin();
			try {
				source.subscribe(observer);
			}finally {
				activeContext.endContext();
			}
		}

	}

}

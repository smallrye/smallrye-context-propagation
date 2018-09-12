package io.smallrye.concurrency.propagators.rxjava2;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.functions.Function;
import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;

public class ContextPropagatorOnCompletableAssemblyAction implements Function<Completable, Completable> {

	@Override
	public Completable apply(Completable t) throws Exception {
		return new ContextPropagatorCompletable(t);
	}

	public class ContextPropagatorCompletable extends Completable {

		private Completable source;
		private CapturedContextState capturedContext;

		public ContextPropagatorCompletable(Completable t) {
			this.source = t;
			this.capturedContext = SmallRyeConcurrencyProvider.captureContext();
		}

		@Override
		protected void subscribeActual(CompletableObserver observer) {
			ActiveContextState activeContext = capturedContext.begin();
			try {
				source.subscribe(observer);
			}finally {
				activeContext.endContext();
			}
		}

	}

}

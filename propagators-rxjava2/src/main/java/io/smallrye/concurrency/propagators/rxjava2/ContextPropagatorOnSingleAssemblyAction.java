package io.smallrye.concurrency.propagators.rxjava2;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.functions.Function;
import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;

public class ContextPropagatorOnSingleAssemblyAction implements Function<Single, Single> {

	@Override
	public Single apply(Single t) throws Exception {
		return new ContextPropagatorSingle(t);
	}

	public class ContextPropagatorSingle<T> extends Single<T> {

		private Single<T> source;
		private CapturedContextState capturedContext;

		public ContextPropagatorSingle(Single<T> t) {
			this.source = t;
			this.capturedContext = SmallRyeConcurrencyProvider.captureContext();
		}

		@Override
		protected void subscribeActual(SingleObserver<? super T> observer) {
			ActiveContextState activeContext = capturedContext.begin();
			try {
				source.subscribe(observer);
			}finally {
				activeContext.endContext();
			}
		}

	}

}

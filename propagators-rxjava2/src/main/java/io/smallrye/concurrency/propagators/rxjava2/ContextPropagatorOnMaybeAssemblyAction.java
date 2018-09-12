package io.smallrye.concurrency.propagators.rxjava2;

import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.functions.Function;
import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;

public class ContextPropagatorOnMaybeAssemblyAction implements Function<Maybe, Maybe> {

	@Override
	public Maybe apply(Maybe t) throws Exception {
		return new ContextPropagatorMaybe(t);
	}

	public class ContextPropagatorMaybe<T> extends Maybe<T> {

		private Maybe<T> source;
		private CapturedContextState capturedContext;

		public ContextPropagatorMaybe(Maybe<T> t) {
			this.source = t;
			this.capturedContext = SmallRyeConcurrencyProvider.captureContext();
		}

		@Override
		protected void subscribeActual(MaybeObserver<? super T> observer) {
			ActiveContextState activeContext = capturedContext.begin();
			try {
				source.subscribe(observer);
			}finally {
				activeContext.endContext();
			}
		}

	}

}

package io.smallrye.concurrency.propagators.rxjava2;

import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.smallrye.concurrency.ActiveContextState;
import io.smallrye.concurrency.CapturedContextState;
import io.smallrye.concurrency.SmallRyeConcurrencyProvider;

public class ContextPropagatorOnFlowableAssemblyAction implements Function<Flowable, Flowable> {

	@Override
	public Flowable apply(Flowable t) throws Exception {
		return new ContextPropagatorFlowable(t);
	}

	public class ContextPropagatorFlowable<T> extends Flowable<T> {

		private Flowable<T> source;
		private CapturedContextState capturedContext;

		public ContextPropagatorFlowable(Flowable<T> t) {
			this.source = t;
			this.capturedContext = SmallRyeConcurrencyProvider.captureContext();
		}

		@Override
		protected void subscribeActual(Subscriber<? super T> observer) {
			ActiveContextState activeContext = capturedContext.begin();
			try {
				source.subscribe(observer);
			}finally {
				activeContext.endContext();
			}
		}

	}

}

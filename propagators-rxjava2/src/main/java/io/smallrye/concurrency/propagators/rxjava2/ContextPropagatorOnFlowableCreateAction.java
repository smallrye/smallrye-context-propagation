package io.smallrye.concurrency.propagators.rxjava2;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.concurrent.ThreadContext;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;

@SuppressWarnings("rawtypes")
public class ContextPropagatorOnFlowableCreateAction
		implements BiFunction<Flowable, Subscriber, Subscriber> {

	private ThreadContext threadContext;

	public ContextPropagatorOnFlowableCreateAction(ThreadContext threadContext) {
		this.threadContext = threadContext;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Subscriber apply(Flowable flowable, Subscriber observer) throws Exception {
		return new ContextCapturerFlowable<>(flowable, observer, threadContext.withCurrentContext());
	}

	public class ContextCapturerFlowable<T> implements Subscriber<T> {

	    private final Subscriber<T> source;
		private final Executor contextExecutor;

		public ContextCapturerFlowable(Flowable<T> observable, Subscriber<T> observer, Executor contextExecutor) {
	    	this.source = observer;
			this.contextExecutor = contextExecutor;
		}

		@Override
		public void onComplete() {
			contextExecutor.execute(() -> source.onComplete());
		}

		@Override
		public void onError(Throwable t) {
			contextExecutor.execute(() -> source.onError(t));
		}

		@Override
		public void onNext(T v) {
			contextExecutor.execute(() -> source.onNext(v));
		}

		@Override
		public void onSubscribe(Subscription s) {
			contextExecutor.execute(() -> source.onSubscribe(s));
		}
	}

}

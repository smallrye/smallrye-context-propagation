package io.smallrye.concurrency.propagators.rxjava2;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.concurrent.ThreadContext;

import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;

@SuppressWarnings("rawtypes")
public class ContextPropagatorOnMaybeCreateAction
		implements BiFunction<Maybe, MaybeObserver, MaybeObserver> {

	private ThreadContext threadContext;

	public ContextPropagatorOnMaybeCreateAction(ThreadContext threadContext) {
		this.threadContext = threadContext;
	}

	@SuppressWarnings("unchecked")
	@Override
	public MaybeObserver apply(Maybe maybe, MaybeObserver observer) throws Exception {
		return new ContextCapturerMaybe<>(maybe, observer, threadContext.currentContextExecutor());
	}

	public class ContextCapturerMaybe<T> implements MaybeObserver<T> {

	    private final MaybeObserver<T> source;
		private final Executor contextExecutor;

		public ContextCapturerMaybe(Maybe<T> observable, MaybeObserver<T> observer, Executor contextExecutor) {
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
		public void onSubscribe(Disposable d) {
			contextExecutor.execute(() -> source.onSubscribe(d));
		}

		@Override
		public void onSuccess(T v) {
			contextExecutor.execute(() -> source.onSuccess(v));
		}
	}

}

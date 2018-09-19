package io.smallrye.concurrency.propagators.rxjava2;

import java.util.concurrent.Executor;

import org.eclipse.microprofile.concurrent.ThreadContext;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.functions.Function;

@SuppressWarnings("rawtypes")
public class ContextPropagatorOnSingleAssemblyAction implements Function<Single, Single> {

	private ThreadContext threadContext;

	public ContextPropagatorOnSingleAssemblyAction(ThreadContext threadContext) {
		this.threadContext = threadContext;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Single apply(Single t) throws Exception {
		return new ContextPropagatorSingle(t, threadContext.withCurrentContext());
	}

	public class ContextPropagatorSingle<T> extends Single<T> {

		private Single<T> source;
		private final Executor contextExecutor;

		public ContextPropagatorSingle(Single<T> t, Executor contextExecutor) {
			this.source = t;
			this.contextExecutor = contextExecutor;
		}

		@Override
		protected void subscribeActual(SingleObserver<? super T> observer) {
			contextExecutor.execute(() -> source.subscribe(observer));
		}

	}

}

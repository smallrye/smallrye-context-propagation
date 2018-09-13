package io.smallrye.concurrency.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.eclipse.microprofile.concurrent.ManagedExecutor;

public class ManagedExecutorImpl extends ThreadPoolExecutor implements ManagedExecutor {

	private ThreadContextImpl threadContext;

	public ManagedExecutorImpl(int maxAsync, int maxQueued, ThreadContextImpl threadContext) {
		super(0, maxAsync, 0l, null, new LinkedBlockingQueue<>(maxQueued));
		this.threadContext = threadContext;
	}

	@Override
	public void shutdown() {
		throw new IllegalStateException("Lifecyle management disallowed");
	}

	@Override
	public List<Runnable> shutdownNow() {
		throw new IllegalStateException("Lifecyle management disallowed");
	}

	@Override
	public boolean isShutdown() {
		throw new IllegalStateException("Lifecyle management disallowed");
	}

	@Override
	public boolean isTerminated() {
		throw new IllegalStateException("Lifecyle management disallowed");
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		throw new IllegalStateException("Lifecyle management disallowed");
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return super.submit(threadContext.withCurrentContext(task));
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return super.submit(threadContext.withCurrentContext(task), result);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return super.submit(threadContext.withCurrentContext(task));
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size());
		for (Callable<T> task : tasks) {
			wrappedTasks.add(threadContext.withCurrentContext(task));
		}
		return super.invokeAll(wrappedTasks);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size());
		for (Callable<T> task : tasks) {
			wrappedTasks.add(threadContext.withCurrentContext(task));
		}
		return super.invokeAll(wrappedTasks, timeout, unit);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size());
		for (Callable<T> task : tasks) {
			wrappedTasks.add(threadContext.withCurrentContext(task));
		}
		return super.invokeAny(wrappedTasks);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size());
		for (Callable<T> task : tasks) {
			wrappedTasks.add(threadContext.withCurrentContext(task));
		}
		return super.invokeAny(wrappedTasks, timeout, unit);
	}

	@Override
	public void execute(Runnable command) {
		super.execute(threadContext.withCurrentContext(command));
	}

	@Override
	public <U> CompletableFuture<U> completedFuture(U value) {
		return threadContext.withCurrentContext(CompletableFuture.completedFuture(value));
	}

	@Override
	public <U> CompletionStage<U> completedStage(U value) {
		return completedFuture(value);
	}

	@Override
	public <U> CompletableFuture<U> failedFuture(Throwable ex) {
		CompletableFuture<U> ret = new CompletableFuture<>();
		ret.completeExceptionally(ex);
		return threadContext.withCurrentContext(ret);
	}

	@Override
	public <U> CompletionStage<U> failedStage(Throwable ex) {
		return failedFuture(ex);
	}

	@Override
	public CompletableFuture<Void> runAsync(Runnable runnable) {
		// I don't need to wrap runnable because this executor will be used to submit the task immediately with
		// a Runnable that will capture the context before calling my Runnable.run
		return threadContext.withCurrentContext(CompletableFuture.runAsync(runnable, this), this);
	}

	@Override
	public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
		// I don't need to wrap supplier because this executor will be used to submit the task immediately with
		// a Runnable that will capture the context before calling my Supplier.run
		return threadContext.withCurrentContext(CompletableFuture.supplyAsync(threadContext.withCurrentContext(supplier), this), this);
	}

}

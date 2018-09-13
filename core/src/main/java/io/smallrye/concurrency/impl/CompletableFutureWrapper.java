package io.smallrye.concurrency.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.microprofile.concurrent.ManagedExecutor;

import io.smallrye.concurrency.CapturedContextState;

final class CompletableFutureWrapper<T> extends CompletableFuture<T> {
	private final CapturedContextState state;
	private final CompletableFuture<T> f;
	private final ThreadContextImpl context;
	private final ManagedExecutor executor;

	CompletableFutureWrapper(ThreadContextImpl context, CapturedContextState state, CompletableFuture<T> f, ManagedExecutor executor) {
		this.context = context;
		this.state = state;
		this.f = f;
		this.executor = executor;
	}

	@Override
	public boolean complete(T value) {
		return f.complete(value);
	}

	@Override
	public boolean completeExceptionally(Throwable ex) {
		return f.completeExceptionally(ex);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return f.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return f.isCancelled();
	}

	@Override
	public boolean isCompletedExceptionally() {
		return f.isCompletedExceptionally();
	}

	@Override
	public void obtrudeValue(T value) {
		f.obtrudeValue(value);
	}

	@Override
	public void obtrudeException(Throwable ex) {
		f.obtrudeException(ex);
	}

	@Override
	public int getNumberOfDependents() {
		return f.getNumberOfDependents();
	}

	@Override
	public boolean isDone() {
		return f.isDone();
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		return f.get();
	}

	@Override
	public T get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return f.get(timeout, unit);
	}

	@Override
	public T join() {
		return f.join();
	}

	@Override
	public T getNow(T valueIfAbsent) {
		return f.getNow(valueIfAbsent);
	}

	@Override
	public CompletableFuture<T> toCompletableFuture() {
		return this;
	}

	@Override
	public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
		return context.withContext(state, f.exceptionally(context.withContext(state, fn)), executor);
	}

	@Override
	public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
		return context.withContext(state, f.handle(context.withContext(state, fn)), executor);
	}

	@Override
	public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
		return context.withContext(state, f.handleAsync(context.withContext(state, fn), executor), executor);
	}

	@Override
	public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn,
			Executor executor) {
		return context.withContext(state, f.handleAsync(context.withContext(state, fn), executor), this.executor);
	}

	@Override
	public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
		return context.withContext(state, f.thenApply(context.withContext(state, fn)), executor);
	}

	@Override
	public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
		return context.withContext(state, f.thenApplyAsync(context.withContext(state, fn), executor), executor);
	}

	@Override
	public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
		return context.withContext(state, f.thenApplyAsync(context.withContext(state, fn), executor), this.executor);
	}

	@Override
	public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
		return context.withContext(state, f.thenAccept(context.withContext(state, action)), executor);
	}

	@Override
	public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
		return context.withContext(state, f.thenAcceptAsync(context.withContext(state, action), executor), executor);
	}

	@Override
	public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
		return context.withContext(state, f.thenAcceptAsync(context.withContext(state, action), executor), this.executor);
	}

	@Override
	public CompletableFuture<Void> thenRun(Runnable action) {
		return context.withContext(state, f.thenRun(context.withContext(state, action)), executor);
	}

	@Override
	public CompletableFuture<Void> thenRunAsync(Runnable action) {
		return context.withContext(state, f.thenRunAsync(context.withContext(state, action), executor), executor);
	}

	@Override
	public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
		return context.withContext(state, f.thenRunAsync(context.withContext(state, action), executor), this.executor);
	}

	@Override
	public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other,
			BiFunction<? super T, ? super U, ? extends V> fn) {
		return context.withContext(state, f.thenCombine(other, context.withContext(state, fn)), executor);
	}

	@Override
	public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
			BiFunction<? super T, ? super U, ? extends V> fn) {
		return context.withContext(state, f.thenCombineAsync(other, context.withContext(state, fn), executor), executor);
	}

	@Override
	public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
			BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
		return context.withContext(state, f.thenCombineAsync(other, context.withContext(state, fn), executor), this.executor);
	}

	@Override
	public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other,
			BiConsumer<? super T, ? super U> action) {
		return context.withContext(state, f.thenAcceptBoth(other, context.withContext(state, action)), executor);
	}

	@Override
	public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
			BiConsumer<? super T, ? super U> action) {
		return context.withContext(state, f.thenAcceptBothAsync(other, context.withContext(state, action), executor), executor);
	}

	@Override
	public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
			BiConsumer<? super T, ? super U> action, Executor executor) {
		return context.withContext(state, f.thenAcceptBothAsync(other, context.withContext(state, action), executor), this.executor);
	}

	@Override
	public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
		return context.withContext(state, f.runAfterBoth(other, context.withContext(state, action)), executor);
	}

	@Override
	public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
		return context.withContext(state, f.runAfterBothAsync(other, context.withContext(state, action), executor), executor);
	}

	@Override
	public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action,
			Executor executor) {
		return context.withContext(state, f.runAfterBothAsync(other, context.withContext(state, action), executor), this.executor);
	}

	@Override
	public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other,
			Function<? super T, U> fn) {
		return context.withContext(state, f.applyToEither(other, context.withContext(state, fn)), executor);
	}

	@Override
	public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other,
			Function<? super T, U> fn) {
		return context.withContext(state, f.applyToEitherAsync(other, context.withContext(state, fn), executor), executor);
	}

	@Override
	public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other,
			Function<? super T, U> fn, Executor executor) {
		return context.withContext(state, f.applyToEitherAsync(other, context.withContext(state, fn), executor), this.executor);
	}

	@Override
	public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other,
			Consumer<? super T> action) {
		return context.withContext(state, f.acceptEither(other, context.withContext(state, action)), executor);
	}

	@Override
	public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other,
			Consumer<? super T> action) {
		return context.withContext(state, f.acceptEitherAsync(other, context.withContext(state, action), executor), executor);
	}

	@Override
	public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other,
			Consumer<? super T> action, Executor executor) {
		return context.withContext(state, f.acceptEitherAsync(other, context.withContext(state, action), executor), this.executor);
	}

	@Override
	public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
		return context.withContext(state, f.runAfterEither(other, context.withContext(state, action)), executor);
	}

	@Override
	public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
		return context.withContext(state, f.runAfterEitherAsync(other, context.withContext(state, action), executor), executor);
	}

	@Override
	public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action,
			Executor executor) {
		return context.withContext(state, f.runAfterEitherAsync(other, context.withContext(state, action), executor), this.executor);
	}

	@Override
	public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
		return context.withContext(state, f.thenCompose(context.withContext(state, fn)), executor);
	}

	@Override
	public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
		return context.withContext(state, f.thenComposeAsync(context.withContext(state, fn), executor), executor);
	}

	@Override
	public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
			Executor executor) {
		return context.withContext(state, f.thenComposeAsync(context.withContext(state, fn), executor), this.executor);
	}

	@Override
	public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
		return context.withContext(state, f.whenComplete(context.withContext(state, action)), executor);
	}

	@Override
	public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
		return context.withContext(state, f.whenCompleteAsync(context.withContext(state, action), executor), executor);
	}

	@Override
	public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action,
			Executor executor) {
		return context.withContext(state, f.whenCompleteAsync(context.withContext(state, action), executor), this.executor);
	}

	@Override
	public String toString() {
		return f.toString();
	}

	@Override
	public int hashCode() {
		return f.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return f.equals(obj);
	}
}
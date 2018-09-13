package io.smallrye.concurrency.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import io.smallrye.concurrency.CapturedContextState;

final class CompletionStageWrapper<T> implements CompletionStage<T> {
	private final CapturedContextState state;
	private final CompletionStage<T> f;
	private final ThreadContextImpl context;

	CompletionStageWrapper(ThreadContextImpl context, CapturedContextState state, CompletionStage<T> f) {
		this.context = context;
		this.state = state;
		this.f = f;
	}

	@Override
	public CompletableFuture<T> toCompletableFuture() {
		// FIXME: propagate an executor too here?
		return context.withContext(state, f.toCompletableFuture(), null);
	}

	@Override
	public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn) {
		return context.withContext(state, f.exceptionally(context.withContext(state, fn)));
	}

	@Override
	public <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
		return context.withContext(state, f.handle(context.withContext(state, fn)));
	}

	@Override
	public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
		return context.withContext(state, f.handleAsync(context.withContext(state, fn)));
	}

	@Override
	public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn,
			Executor executor) {
		return context.withContext(state, f.handleAsync(context.withContext(state, fn), executor));
	}

	@Override
	public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
		return context.withContext(state, f.thenApply(context.withContext(state, fn)));
	}

	@Override
	public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
		return context.withContext(state, f.thenApplyAsync(context.withContext(state, fn)));
	}

	@Override
	public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
		return context.withContext(state, f.thenApplyAsync(context.withContext(state, fn), executor));
	}

	@Override
	public CompletionStage<Void> thenAccept(Consumer<? super T> action) {
		return context.withContext(state, f.thenAccept(context.withContext(state, action)));
	}

	@Override
	public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action) {
		return context.withContext(state, f.thenAcceptAsync(context.withContext(state, action)));
	}

	@Override
	public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
		return context.withContext(state, f.thenAcceptAsync(context.withContext(state, action), executor));
	}

	@Override
	public CompletionStage<Void> thenRun(Runnable action) {
		return context.withContext(state, f.thenRun(context.withContext(state, action)));
	}

	@Override
	public CompletionStage<Void> thenRunAsync(Runnable action) {
		return context.withContext(state, f.thenRunAsync(context.withContext(state, action)));
	}

	@Override
	public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
		return context.withContext(state, f.thenRunAsync(context.withContext(state, action), executor));
	}

	@Override
	public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
			BiFunction<? super T, ? super U, ? extends V> fn) {
		return context.withContext(state, f.thenCombine(other, context.withContext(state, fn)));
	}

	@Override
	public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
			BiFunction<? super T, ? super U, ? extends V> fn) {
		return context.withContext(state, f.thenCombineAsync(other, context.withContext(state, fn)));
	}

	@Override
	public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
			BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
		return context.withContext(state, f.thenCombineAsync(other, context.withContext(state, fn), executor));
	}

	@Override
	public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
			BiConsumer<? super T, ? super U> action) {
		return context.withContext(state, f.thenAcceptBoth(other, context.withContext(state, action)));
	}

	@Override
	public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
			BiConsumer<? super T, ? super U> action) {
		return context.withContext(state, f.thenAcceptBothAsync(other, context.withContext(state, action)));
	}

	@Override
	public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
			BiConsumer<? super T, ? super U> action, Executor executor) {
		return context.withContext(state, f.thenAcceptBothAsync(other, context.withContext(state, action), executor));
	}

	@Override
	public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
		return context.withContext(state, f.runAfterBoth(other, context.withContext(state, action)));
	}

	@Override
	public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
		return context.withContext(state, f.runAfterBothAsync(other, context.withContext(state, action)));
	}

	@Override
	public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action,
			Executor executor) {
		return context.withContext(state, f.runAfterBothAsync(other, context.withContext(state, action), executor));
	}

	@Override
	public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other,
			Function<? super T, U> fn) {
		return context.withContext(state, f.applyToEither(other, context.withContext(state, fn)));
	}

	@Override
	public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
			Function<? super T, U> fn) {
		return context.withContext(state, f.applyToEitherAsync(other, context.withContext(state, fn)));
	}

	@Override
	public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
			Function<? super T, U> fn, Executor executor) {
		return context.withContext(state, f.applyToEitherAsync(other, context.withContext(state, fn), executor));
	}

	@Override
	public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other,
			Consumer<? super T> action) {
		return context.withContext(state, f.acceptEither(other, context.withContext(state, action)));
	}

	@Override
	public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
			Consumer<? super T> action) {
		return context.withContext(state, f.acceptEitherAsync(other, context.withContext(state, action)));
	}

	@Override
	public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
			Consumer<? super T> action, Executor executor) {
		return context.withContext(state, f.acceptEitherAsync(other, context.withContext(state, action), executor));
	}

	@Override
	public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
		return context.withContext(state, f.runAfterEither(other, context.withContext(state, action)));
	}

	@Override
	public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
		return context.withContext(state, f.runAfterEitherAsync(other, context.withContext(state, action)));
	}

	@Override
	public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action,
			Executor executor) {
		return context.withContext(state, f.runAfterEitherAsync(other, context.withContext(state, action), executor));
	}

	@Override
	public <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
		return context.withContext(state, f.thenCompose(context.withContext(state, fn)));
	}

	@Override
	public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
		return context.withContext(state, f.thenComposeAsync(context.withContext(state, fn)));
	}

	@Override
	public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
			Executor executor) {
		return context.withContext(state, f.thenComposeAsync(context.withContext(state, fn), executor));
	}

	@Override
	public CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
		return context.withContext(state, f.whenComplete(context.withContext(state, action)));
	}

	@Override
	public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
		return context.withContext(state, f.whenCompleteAsync(context.withContext(state, action)));
	}

	@Override
	public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action,
			Executor executor) {
		return context.withContext(state, f.whenCompleteAsync(context.withContext(state, action), executor));
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
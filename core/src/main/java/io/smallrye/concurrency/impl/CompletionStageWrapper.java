package io.smallrye.concurrency.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

final class CompletionStageWrapper<T> implements CompletionStage<T> {
	private final CompletionStage<T> f;
	private final ThreadContextImpl context;

	CompletionStageWrapper(ThreadContextImpl context, CompletionStage<T> f) {
		this.context = context;
		this.f = f;
	}

	@Override
	public CompletableFuture<T> toCompletableFuture() {
		// FIXME: propagate an executor too here?
		return context.withContextCapture(f.toCompletableFuture(), null);
	}

	@Override
	public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn) {
		return context.withContextCapture(f.exceptionally(context.withCurrentContext(fn)));
	}

	@Override
	public <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
		return context.withContextCapture(f.handle(context.withCurrentContext(fn)));
	}

	@Override
	public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
		return context.withContextCapture(f.handleAsync(context.withCurrentContext(fn)));
	}

	@Override
	public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn,
			Executor executor) {
		return context.withContextCapture(f.handleAsync(context.withCurrentContext(fn), executor));
	}

	@Override
	public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
		return context.withContextCapture(f.thenApply(context.withCurrentContext(fn)));
	}

	@Override
	public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
		return context.withContextCapture(f.thenApplyAsync(context.withCurrentContext(fn)));
	}

	@Override
	public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
		return context.withContextCapture(f.thenApplyAsync(context.withCurrentContext(fn), executor));
	}

	@Override
	public CompletionStage<Void> thenAccept(Consumer<? super T> action) {
		return context.withContextCapture(f.thenAccept(context.withCurrentContext(action)));
	}

	@Override
	public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action) {
		return context.withContextCapture(f.thenAcceptAsync(context.withCurrentContext(action)));
	}

	@Override
	public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
		return context.withContextCapture(f.thenAcceptAsync(context.withCurrentContext(action), executor));
	}

	@Override
	public CompletionStage<Void> thenRun(Runnable action) {
		return context.withContextCapture(f.thenRun(context.withCurrentContext(action)));
	}

	@Override
	public CompletionStage<Void> thenRunAsync(Runnable action) {
		return context.withContextCapture(f.thenRunAsync(context.withCurrentContext(action)));
	}

	@Override
	public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
		return context.withContextCapture(f.thenRunAsync(context.withCurrentContext(action), executor));
	}

	@Override
	public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
			BiFunction<? super T, ? super U, ? extends V> fn) {
		return context.withContextCapture(f.thenCombine(other, context.withCurrentContext(fn)));
	}

	@Override
	public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
			BiFunction<? super T, ? super U, ? extends V> fn) {
		return context.withContextCapture(f.thenCombineAsync(other, context.withCurrentContext(fn)));
	}

	@Override
	public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
			BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
		return context.withContextCapture(f.thenCombineAsync(other, context.withCurrentContext(fn), executor));
	}

	@Override
	public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
			BiConsumer<? super T, ? super U> action) {
		return context.withContextCapture(f.thenAcceptBoth(other, context.withCurrentContext(action)));
	}

	@Override
	public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
			BiConsumer<? super T, ? super U> action) {
		return context.withContextCapture(f.thenAcceptBothAsync(other, context.withCurrentContext(action)));
	}

	@Override
	public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
			BiConsumer<? super T, ? super U> action, Executor executor) {
		return context.withContextCapture(f.thenAcceptBothAsync(other, context.withCurrentContext(action), executor));
	}

	@Override
	public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
		return context.withContextCapture(f.runAfterBoth(other, context.withCurrentContext(action)));
	}

	@Override
	public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
		return context.withContextCapture(f.runAfterBothAsync(other, context.withCurrentContext(action)));
	}

	@Override
	public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action,
			Executor executor) {
		return context.withContextCapture(f.runAfterBothAsync(other, context.withCurrentContext(action), executor));
	}

	@Override
	public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other,
			Function<? super T, U> fn) {
		return context.withContextCapture(f.applyToEither(other, context.withCurrentContext(fn)));
	}

	@Override
	public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
			Function<? super T, U> fn) {
		return context.withContextCapture(f.applyToEitherAsync(other, context.withCurrentContext(fn)));
	}

	@Override
	public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
			Function<? super T, U> fn, Executor executor) {
		return context.withContextCapture(f.applyToEitherAsync(other, context.withCurrentContext(fn), executor));
	}

	@Override
	public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other,
			Consumer<? super T> action) {
		return context.withContextCapture(f.acceptEither(other, context.withCurrentContext(action)));
	}

	@Override
	public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
			Consumer<? super T> action) {
		return context.withContextCapture(f.acceptEitherAsync(other, context.withCurrentContext(action)));
	}

	@Override
	public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
			Consumer<? super T> action, Executor executor) {
		return context.withContextCapture(f.acceptEitherAsync(other, context.withCurrentContext(action), executor));
	}

	@Override
	public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
		return context.withContextCapture(f.runAfterEither(other, context.withCurrentContext(action)));
	}

	@Override
	public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
		return context.withContextCapture(f.runAfterEitherAsync(other, context.withCurrentContext(action)));
	}

	@Override
	public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action,
			Executor executor) {
		return context.withContextCapture(f.runAfterEitherAsync(other, context.withCurrentContext(action), executor));
	}

	@Override
	public <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
		return context.withContextCapture(f.thenCompose(context.withCurrentContext(fn)));
	}

	@Override
	public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
		return context.withContextCapture(f.thenComposeAsync(context.withCurrentContext(fn)));
	}

	@Override
	public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
			Executor executor) {
		return context.withContextCapture(f.thenComposeAsync(context.withCurrentContext(fn), executor));
	}

	@Override
	public CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
		return context.withContextCapture(f.whenComplete(context.withCurrentContext(action)));
	}

	@Override
	public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
		return context.withContextCapture(f.whenCompleteAsync(context.withCurrentContext(action)));
	}

	@Override
	public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action,
			Executor executor) {
		return context.withContextCapture(f.whenCompleteAsync(context.withCurrentContext(action), executor));
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
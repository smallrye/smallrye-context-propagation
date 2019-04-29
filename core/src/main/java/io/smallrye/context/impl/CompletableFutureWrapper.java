package io.smallrye.context.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.microprofile.context.ManagedExecutor;

final class CompletableFutureWrapper<T> extends CompletableFuture<T> {
    private final CompletableFuture<T> f;
    private final ThreadContextImpl context;
    /**
     * If this executor is not null, we're wrapping a CF. If it is null, we're a dependent stage of
     * another CF, so we have different behaviour
     */
    private final ManagedExecutor executor;

    CompletableFutureWrapper(ThreadContextImpl context, CompletableFuture<T> f, ManagedExecutor executor) {
        this.context = context;
        this.f = f;
        f.whenComplete((r, t) -> {
            if (t != null) {
                if (t instanceof CompletionException)
                    t = t.getCause();
                super.completeExceptionally(t);
            } else
                super.complete(r);
        });
        this.executor = executor;
    }

    private void checkDefaultExecutor() {
        if (executor == null)
            throw new UnsupportedOperationException("Async methods not supported when no executor is specified");
    }

    @Override
    public boolean complete(T value) {
        // dependent stage
        if(executor == null)
            return super.complete(value);
        // wrapper
        return f.complete(value);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        // dependent stage
        if(executor == null)
            return super.completeExceptionally(ex);
        // wrapper
        return f.completeExceptionally(ex);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // dependent stage
        if(executor == null)
            return super.cancel(mayInterruptIfRunning);
        // wrapper
        return f.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        // dependent stage
        if(executor == null)
            return super.isCancelled();
        // wrapper
        return f.isCancelled();
    }

    @Override
    public boolean isCompletedExceptionally() {
        // dependent stage
        if(executor == null)
            return super.isCompletedExceptionally();
        // wrapper
        return f.isCompletedExceptionally();
    }

    @Override
    public void obtrudeValue(T value) {
        // dependent stage
        if(executor == null)
            super.obtrudeValue(value);
        else
            // wrapper
            f.obtrudeValue(value);
    }

    @Override
    public void obtrudeException(Throwable ex) {
        // dependent stage
        if(executor == null)
            super.obtrudeException(ex);
        else
            // wrapper
            f.obtrudeException(ex);
    }

    @Override
    public int getNumberOfDependents() {
        // dependent stage
        if(executor == null)
            return super.getNumberOfDependents();
        // wrapper
        return f.getNumberOfDependents();
    }

    @Override
    public boolean isDone() {
        // dependent stage
        if(executor == null)
            return super.isDone();
        // wrapper
        return f.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        // dependent stage
        if(executor == null)
            return super.get();
        // wrapper
        return f.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        // dependent stage
        if(executor == null)
            return super.get(timeout, unit);
        // wrapper
        return f.get(timeout, unit);
    }

    @Override
    public T join() {
        // dependent stage
        if(executor == null)
            return super.join();
        // wrapper
        return f.join();
    }

    @Override
    public T getNow(T valueIfAbsent) {
        // dependent stage
        if(executor == null)
            return super.getNow(valueIfAbsent);
        // wrapper
        return f.getNow(valueIfAbsent);
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    @Override
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return context.withContextCapture(f.exceptionally(context.contextualFunctionUnlessContextualized(fn)), executor);
    }

    @Override
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return context.withContextCapture(f.handle(context.contextualFunctionUnlessContextualized(fn)), executor);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(f.handleAsync(context.contextualFunctionUnlessContextualized(fn), executor), executor);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return context.withContextCapture(f.handleAsync(context.contextualFunctionUnlessContextualized(fn), executor), this.executor);
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return context.withContextCapture(f.thenApply(context.contextualFunctionUnlessContextualized(fn)), executor);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(f.thenApplyAsync(context.contextualFunctionUnlessContextualized(fn), executor), executor);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return context.withContextCapture(f.thenApplyAsync(context.contextualFunctionUnlessContextualized(fn), executor), this.executor);
    }

    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return context.withContextCapture(f.thenAccept(context.contextualConsumerUnlessContextualized(action)), executor);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        checkDefaultExecutor();
        return context.withContextCapture(f.thenAcceptAsync(context.contextualConsumerUnlessContextualized(action), executor), executor);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return context.withContextCapture(f.thenAcceptAsync(context.contextualConsumerUnlessContextualized(action), executor), this.executor);
    }

    @Override
    public CompletableFuture<Void> thenRun(Runnable action) {
        return context.withContextCapture(f.thenRun(context.contextualRunnableUnlessContextualized(action)), executor);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        checkDefaultExecutor();
        return context.withContextCapture(f.thenRunAsync(context.contextualRunnableUnlessContextualized(action), executor), executor);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return context.withContextCapture(f.thenRunAsync(context.contextualRunnableUnlessContextualized(action), executor), this.executor);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        return context.withContextCapture(f.thenCombine(other, context.contextualFunctionUnlessContextualized(fn)), executor);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(f.thenCombineAsync(other, context.contextualFunctionUnlessContextualized(fn), executor),
                executor);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return context.withContextCapture(f.thenCombineAsync(other, context.contextualFunctionUnlessContextualized(fn), executor), this.executor);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return context.withContextCapture(f.thenAcceptBoth(other, context.contextualConsumerUnlessContextualized(action)), executor);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        checkDefaultExecutor();
        return context.withContextCapture(f.thenAcceptBothAsync(other, context.contextualConsumerUnlessContextualized(action), executor),
                executor);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action, Executor executor) {
        return context.withContextCapture(f.thenAcceptBothAsync(other, context.contextualConsumerUnlessContextualized(action), executor), this.executor);
    }

    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return context.withContextCapture(f.runAfterBoth(other, context.contextualRunnableUnlessContextualized(action)), executor);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        checkDefaultExecutor();
        return context.withContextCapture(f.runAfterBothAsync(other, context.contextualRunnableUnlessContextualized(action), executor),
                executor);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return context.withContextCapture(f.runAfterBothAsync(other, context.contextualRunnableUnlessContextualized(action), executor), this.executor);
    }

    @Override
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return context.withContextCapture(f.applyToEither(other, context.contextualFunctionUnlessContextualized(fn)), executor);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(f.applyToEitherAsync(other, context.contextualFunctionUnlessContextualized(fn), executor),
                executor);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn,
            Executor executor) {
        return context.withContextCapture(f.applyToEitherAsync(other, context.contextualFunctionUnlessContextualized(fn), executor), this.executor);
    }

    @Override
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return context.withContextCapture(f.acceptEither(other, context.contextualConsumerUnlessContextualized(action)), executor);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        checkDefaultExecutor();
        return context.withContextCapture(f.acceptEitherAsync(other, context.contextualConsumerUnlessContextualized(action), executor),
                executor);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action,
            Executor executor) {
        return context.withContextCapture(f.acceptEitherAsync(other, context.contextualConsumerUnlessContextualized(action), executor), this.executor);
    }

    @Override
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return context.withContextCapture(f.runAfterEither(other, context.contextualRunnableUnlessContextualized(action)), executor);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        checkDefaultExecutor();
        return context.withContextCapture(f.runAfterEitherAsync(other, context.contextualRunnableUnlessContextualized(action), executor),
                executor);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return context.withContextCapture(f.runAfterEitherAsync(other, context.contextualRunnableUnlessContextualized(action), executor), this.executor);
    }

    @Override
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return context.withContextCapture(f.thenCompose(context.contextualFunctionUnlessContextualized(fn)), executor);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(f.thenComposeAsync(context.contextualFunctionUnlessContextualized(fn), executor), executor);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
            Executor executor) {
        return context.withContextCapture(f.thenComposeAsync(context.contextualFunctionUnlessContextualized(fn), executor), this.executor);
    }

    @Override
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return context.withContextCapture(f.whenComplete(context.contextualConsumerUnlessContextualized(action)), executor);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        checkDefaultExecutor();
        return context.withContextCapture(f.whenCompleteAsync(context.contextualConsumerUnlessContextualized(action), executor), executor);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return context.withContextCapture(f.whenCompleteAsync(context.contextualConsumerUnlessContextualized(action), executor), this.executor);
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
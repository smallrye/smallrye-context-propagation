package io.smallrye.context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.context.impl.Contextualized;

public class ContextualCompletableFuture<T> extends CompletableFuture<T> implements Contextualized {
    protected final SmallRyeThreadContext context;
    protected final Executor executor;
    protected final boolean minimal;

    public ContextualCompletableFuture(SmallRyeThreadContext context, Executor executor, boolean minimal) {
        this.context = context;
        this.executor = executor;
        this.minimal = minimal;
    }

    protected void checkDefaultExecutor() {
        if (executor == null)
            throw new UnsupportedOperationException("Async methods not supported when no executor is specified");
    }

    private void checkMinimal() {
        if (minimal)
            throw new UnsupportedOperationException("Completion methods not supported for minimal CompletionStage instances");
    }

    @Override
    public boolean complete(T value) {
        checkMinimal();
        return super.complete(value);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        checkMinimal();
        return super.completeExceptionally(ex);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        checkMinimal();
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public void obtrudeValue(T value) {
        checkMinimal();
        super.obtrudeValue(value);
    }

    @Override
    public void obtrudeException(Throwable ex) {
        checkMinimal();
        super.obtrudeException(ex);
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        if (minimal) {
            CompletableFuture<T> ret = JdkSpecific.newCompletableFuture(context, executor);
            super.whenComplete((val, x) -> {
                if (x != null) {
                    ret.completeExceptionally(x);
                } else {
                    ret.complete(val);
                }
            });
            return ret;
        } else {
            return this;
        }
    }

    @Override
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return super.exceptionally(context.contextualFunctionUnlessContextualized(fn));
    }

    @Override
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return super.handle(context.contextualFunctionUnlessContextualized(fn));
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        checkDefaultExecutor();
        return super.handleAsync(context.contextualFunctionUnlessContextualized(fn), executor);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return super.handleAsync(context.contextualFunctionUnlessContextualized(fn), executor);
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return super.thenApply(context.contextualFunctionUnlessContextualized(fn));
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        checkDefaultExecutor();
        return super.thenApplyAsync(context.contextualFunctionUnlessContextualized(fn), executor);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return super.thenApplyAsync(context.contextualFunctionUnlessContextualized(fn), executor);
    }

    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return super.thenAccept(context.contextualConsumerUnlessContextualized(action));
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        checkDefaultExecutor();
        return super.thenAcceptAsync(context.contextualConsumerUnlessContextualized(action), executor);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return super.thenAcceptAsync(context.contextualConsumerUnlessContextualized(action), executor);
    }

    @Override
    public CompletableFuture<Void> thenRun(Runnable action) {
        return super.thenRun(context.contextualRunnableUnlessContextualized(action));
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        checkDefaultExecutor();
        return super.thenRunAsync(context.contextualRunnableUnlessContextualized(action), executor);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return super.thenRunAsync(context.contextualRunnableUnlessContextualized(action), executor);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        return super.thenCombine(other, context.contextualFunctionUnlessContextualized(fn));
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        checkDefaultExecutor();
        return super.thenCombineAsync(other, context.contextualFunctionUnlessContextualized(fn), executor);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return super.thenCombineAsync(other, context.contextualFunctionUnlessContextualized(fn), executor);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return super.thenAcceptBoth(other, context.contextualConsumerUnlessContextualized(action));
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        checkDefaultExecutor();
        return super.thenAcceptBothAsync(other, context.contextualConsumerUnlessContextualized(action), executor);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action, Executor executor) {
        return super.thenAcceptBothAsync(other, context.contextualConsumerUnlessContextualized(action), executor);
    }

    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return super.runAfterBoth(other, context.contextualRunnableUnlessContextualized(action));
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        checkDefaultExecutor();
        return super.runAfterBothAsync(other, context.contextualRunnableUnlessContextualized(action), executor);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return super.runAfterBothAsync(other, context.contextualRunnableUnlessContextualized(action), executor);
    }

    @Override
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return super.applyToEither(other, context.contextualFunctionUnlessContextualized(fn));
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        checkDefaultExecutor();
        return super.applyToEitherAsync(other, context.contextualFunctionUnlessContextualized(fn), executor);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn,
            Executor executor) {
        return super.applyToEitherAsync(other, context.contextualFunctionUnlessContextualized(fn), executor);
    }

    @Override
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return super.acceptEither(other, context.contextualConsumerUnlessContextualized(action));
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        checkDefaultExecutor();
        return super.acceptEitherAsync(other, context.contextualConsumerUnlessContextualized(action), executor);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action,
            Executor executor) {
        return super.acceptEitherAsync(other, context.contextualConsumerUnlessContextualized(action), executor);
    }

    @Override
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return super.runAfterEither(other, context.contextualRunnableUnlessContextualized(action));
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        checkDefaultExecutor();
        return super.runAfterEitherAsync(other, context.contextualRunnableUnlessContextualized(action), executor);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return super.runAfterEitherAsync(other, context.contextualRunnableUnlessContextualized(action), executor);
    }

    @Override
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return super.thenCompose(context.contextualFunctionUnlessContextualized(fn));
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        checkDefaultExecutor();
        return super.thenComposeAsync(context.contextualFunctionUnlessContextualized(fn), executor);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
            Executor executor) {
        return super.thenComposeAsync(context.contextualFunctionUnlessContextualized(fn), executor);
    }

    @Override
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return super.whenComplete(context.contextualConsumerUnlessContextualized(action));
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        checkDefaultExecutor();
        return super.whenCompleteAsync(context.contextualConsumerUnlessContextualized(action), executor);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return super.whenCompleteAsync(context.contextualConsumerUnlessContextualized(action), executor);
    }

    // Java 9

    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        if (minimal) {
            return (CompletableFuture<U>) JdkSpecific.newCompletionStage(context, executor);
        }
        return JdkSpecific.newCompletableFuture(context, executor);
    }

    @Override
    public Executor defaultExecutor() {
        return executor;
    }

    @Override
    public CompletableFuture<T> copy() {
        // this looks ok to forward to
        return super.copy();
    }

    @Override
    public CompletionStage<T> minimalCompletionStage() {
        return JdkSpecific.newCompletionStage(context, executor);
    }

    @Override
    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
        checkMinimal();
        return super.completeAsync(context.contextualSupplierUnlessContextualized(supplier), executor);
    }

    @Override
    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
        checkMinimal();
        return super.completeAsync(context.contextualSupplierUnlessContextualized(supplier), executor);
    }

    @Override
    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
        checkMinimal();
        return super.orTimeout(timeout, unit);
    }

    @Override
    public CompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit) {
        checkMinimal();
        return super.completeOnTimeout(value, timeout, unit);
    }

    void superCompleteExceptionally(Throwable x) {
        super.completeExceptionally(x);
    }

    void superComplete(T val) {
        super.complete(val);
    }

    // Java 12

    @Override
    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn) {
        checkDefaultExecutor();
        return super.exceptionallyAsync(context.contextualFunctionUnlessContextualized(fn), executor);
    }

    @Override
    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn, Executor executor) {
        return super.exceptionallyAsync(context.contextualFunctionUnlessContextualized(fn), executor);
    }

    @Override
    public CompletableFuture<T> exceptionallyCompose(Function<Throwable, ? extends CompletionStage<T>> fn) {
        return super.exceptionallyCompose(context.contextualFunctionUnlessContextualized(fn));
    }

    @Override
    public CompletableFuture<T> exceptionallyComposeAsync(Function<Throwable, ? extends CompletionStage<T>> fn) {
        checkDefaultExecutor();
        return super.exceptionallyComposeAsync(context.contextualFunctionUnlessContextualized(fn), executor);
    }

    @Override
    public CompletableFuture<T> exceptionallyComposeAsync(Function<Throwable, ? extends CompletionStage<T>> fn,
            Executor executor) {
        return super.exceptionallyComposeAsync(context.contextualFunctionUnlessContextualized(fn), executor);
    }

}

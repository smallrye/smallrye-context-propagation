package io.smallrye.context;

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
import java.util.function.Supplier;

import io.smallrye.context.impl.Contextualized;

public class CompletableFutureWrapper<T> extends CompletableFuture<T> implements Contextualized {
    protected final CompletableFuture<T> f;
    protected final SmallRyeThreadContext context;
    /**
     * If this executor is not null, we're wrapping a CF. If it is null, we're a dependent stage of
     * another CF, so we have different behaviour
     */
    protected final Executor executor;
    protected final int flags;

    public final static int FLAG_MINIMAL = 1 << 0;
    public final static int FLAG_DEPENDENT = 1 << 1;

    public CompletableFutureWrapper(SmallRyeThreadContext context, CompletableFuture<T> f, Executor executor, int flags) {
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
        this.flags = flags;
    }

    private void checkDefaultExecutor() {
        if (executor == null)
            throw new UnsupportedOperationException("Async methods not supported when no executor is specified");
    }

    private void checkMinimal() {
        if ((flags & FLAG_MINIMAL) != 0)
            throw new UnsupportedOperationException("Completion methods not supported for minimal CompletionStage instances");
    }

    private boolean isDependent() {
        return (flags & FLAG_DEPENDENT) != 0;
    }

    @Override
    public boolean complete(T value) {
        checkMinimal();
        // dependent stage
        if (isDependent())
            return super.complete(value);
        // wrapper
        return f.complete(value);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        checkMinimal();
        // dependent stage
        if (isDependent())
            return super.completeExceptionally(ex);
        // wrapper
        return f.completeExceptionally(ex);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // dependent stage
        if (isDependent())
            return super.cancel(mayInterruptIfRunning);
        // wrapper
        return f.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        // dependent stage
        if (isDependent())
            return super.isCancelled();
        // wrapper
        return f.isCancelled();
    }

    @Override
    public boolean isCompletedExceptionally() {
        // dependent stage
        if (isDependent())
            return super.isCompletedExceptionally();
        // wrapper
        return f.isCompletedExceptionally();
    }

    @Override
    public void obtrudeValue(T value) {
        // dependent stage
        if (isDependent())
            super.obtrudeValue(value);
        else
            // wrapper
            f.obtrudeValue(value);
    }

    @Override
    public void obtrudeException(Throwable ex) {
        // dependent stage
        if (isDependent())
            super.obtrudeException(ex);
        else
            // wrapper
            f.obtrudeException(ex);
    }

    @Override
    public int getNumberOfDependents() {
        // dependent stage
        if (isDependent())
            return super.getNumberOfDependents();
        // wrapper
        return f.getNumberOfDependents();
    }

    @Override
    public boolean isDone() {
        // dependent stage
        if (isDependent())
            return super.isDone();
        // wrapper
        return f.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        // dependent stage
        if (isDependent())
            return super.get();
        // wrapper
        return f.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        // dependent stage
        if (isDependent())
            return super.get(timeout, unit);
        // wrapper
        return f.get(timeout, unit);
    }

    @Override
    public T join() {
        // dependent stage
        if (isDependent())
            return super.join();
        // wrapper
        return f.join();
    }

    @Override
    public T getNow(T valueIfAbsent) {
        // dependent stage
        if (isDependent())
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
        return context.withContextCapture(f.exceptionally(context.contextualFunctionUnlessContextualized(fn)), executor, flags);
    }

    @Override
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return context.withContextCapture(f.handle(context.contextualFunctionUnlessContextualized(fn)), executor, flags);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(f.handleAsync(context.contextualFunctionUnlessContextualized(fn), executor),
                executor, flags);
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return context.withContextCapture(f.handleAsync(context.contextualFunctionUnlessContextualized(fn), executor),
                this.executor, flags);
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return context.withContextCapture(f.thenApply(context.contextualFunctionUnlessContextualized(fn)), executor, flags);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(f.thenApplyAsync(context.contextualFunctionUnlessContextualized(fn), executor),
                executor, flags);
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return context.withContextCapture(f.thenApplyAsync(context.contextualFunctionUnlessContextualized(fn), executor),
                this.executor, flags);
    }

    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return context.withContextCapture(f.thenAccept(context.contextualConsumerUnlessContextualized(action)), executor,
                flags);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        checkDefaultExecutor();
        return context.withContextCapture(f.thenAcceptAsync(context.contextualConsumerUnlessContextualized(action), executor),
                executor, flags);
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return context.withContextCapture(f.thenAcceptAsync(context.contextualConsumerUnlessContextualized(action), executor),
                this.executor, flags);
    }

    @Override
    public CompletableFuture<Void> thenRun(Runnable action) {
        return context.withContextCapture(f.thenRun(context.contextualRunnableUnlessContextualized(action)), executor, flags);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        checkDefaultExecutor();
        return context.withContextCapture(f.thenRunAsync(context.contextualRunnableUnlessContextualized(action), executor),
                executor, flags);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return context.withContextCapture(f.thenRunAsync(context.contextualRunnableUnlessContextualized(action), executor),
                this.executor, flags);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        return context.withContextCapture(f.thenCombine(other, context.contextualFunctionUnlessContextualized(fn)), executor,
                flags);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(
                f.thenCombineAsync(other, context.contextualFunctionUnlessContextualized(fn), executor),
                executor, flags);
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return context.withContextCapture(
                f.thenCombineAsync(other, context.contextualFunctionUnlessContextualized(fn), executor), this.executor, flags);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return context.withContextCapture(f.thenAcceptBoth(other, context.contextualConsumerUnlessContextualized(action)),
                executor, flags);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        checkDefaultExecutor();
        return context.withContextCapture(
                f.thenAcceptBothAsync(other, context.contextualConsumerUnlessContextualized(action), executor),
                executor, flags);
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action, Executor executor) {
        return context.withContextCapture(
                f.thenAcceptBothAsync(other, context.contextualConsumerUnlessContextualized(action), executor), this.executor,
                flags);
    }

    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return context.withContextCapture(f.runAfterBoth(other, context.contextualRunnableUnlessContextualized(action)),
                executor, flags);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        checkDefaultExecutor();
        return context.withContextCapture(
                f.runAfterBothAsync(other, context.contextualRunnableUnlessContextualized(action), executor),
                executor, flags);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return context.withContextCapture(
                f.runAfterBothAsync(other, context.contextualRunnableUnlessContextualized(action), executor), this.executor,
                flags);
    }

    @Override
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return context.withContextCapture(f.applyToEither(other, context.contextualFunctionUnlessContextualized(fn)), executor,
                flags);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(
                f.applyToEitherAsync(other, context.contextualFunctionUnlessContextualized(fn), executor),
                executor, flags);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn,
            Executor executor) {
        return context.withContextCapture(
                f.applyToEitherAsync(other, context.contextualFunctionUnlessContextualized(fn), executor), this.executor,
                flags);
    }

    @Override
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return context.withContextCapture(f.acceptEither(other, context.contextualConsumerUnlessContextualized(action)),
                executor, flags);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        checkDefaultExecutor();
        return context.withContextCapture(
                f.acceptEitherAsync(other, context.contextualConsumerUnlessContextualized(action), executor),
                executor, flags);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action,
            Executor executor) {
        return context.withContextCapture(
                f.acceptEitherAsync(other, context.contextualConsumerUnlessContextualized(action), executor), this.executor,
                flags);
    }

    @Override
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return context.withContextCapture(f.runAfterEither(other, context.contextualRunnableUnlessContextualized(action)),
                executor, flags);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        checkDefaultExecutor();
        return context.withContextCapture(
                f.runAfterEitherAsync(other, context.contextualRunnableUnlessContextualized(action), executor),
                executor, flags);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return context.withContextCapture(
                f.runAfterEitherAsync(other, context.contextualRunnableUnlessContextualized(action), executor), this.executor,
                flags);
    }

    @Override
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return context.withContextCapture(f.thenCompose(context.contextualFunctionUnlessContextualized(fn)), executor, flags);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(f.thenComposeAsync(context.contextualFunctionUnlessContextualized(fn), executor),
                executor, flags);
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
            Executor executor) {
        return context.withContextCapture(f.thenComposeAsync(context.contextualFunctionUnlessContextualized(fn), executor),
                this.executor, flags);
    }

    @Override
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return context.withContextCapture(f.whenComplete(context.contextualConsumerUnlessContextualized(action)), executor,
                flags);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        checkDefaultExecutor();
        return context.withContextCapture(f.whenCompleteAsync(context.contextualConsumerUnlessContextualized(action), executor),
                executor, flags);
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return context.withContextCapture(f.whenCompleteAsync(context.contextualConsumerUnlessContextualized(action), executor),
                this.executor, flags);
    }

    @Override
    public CompletableFuture<T> completeAsync(Supplier<? extends T> action) {
        checkDefaultExecutor();
        return context.withContextCapture(f.completeAsync(context.contextualSupplierUnlessContextualized(action)),
                this.executor, flags);
    }

    @Override
    public CompletableFuture<T> completeAsync(Supplier<? extends T> action, Executor executor) {
        return context.withContextCapture(f.completeAsync(context.contextualSupplierUnlessContextualized(action)),
                this.executor, flags);
    }

    @Override
    public CompletableFuture<T> exceptionallyComposeAsync(Function<Throwable, ? extends CompletionStage<T>> action) {
        checkDefaultExecutor();
        return context.withContextCapture(f.exceptionallyComposeAsync(context.contextualFunctionUnlessContextualized(action)),
                this.executor, flags);
    }

    @Override
    public CompletableFuture<T> exceptionallyComposeAsync(Function<Throwable, ? extends CompletionStage<T>> action,
            Executor executor) {
        return context.withContextCapture(f.exceptionallyComposeAsync(context.contextualFunctionUnlessContextualized(action)),
                this.executor, flags);
    }

    @Override
    public CompletableFuture<T> exceptionallyCompose(Function<Throwable, ? extends CompletionStage<T>> action) {
        return context.withContextCapture(f.exceptionallyCompose(context.contextualFunctionUnlessContextualized(action)),
                this.executor, flags);
    }

    @Override
    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> action) {
        checkDefaultExecutor();
        return context.withContextCapture(f.exceptionallyAsync(context.contextualFunctionUnlessContextualized(action)),
                this.executor, flags);
    }

    @Override
    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> action, Executor executor) {
        return context.withContextCapture(f.exceptionallyAsync(context.contextualFunctionUnlessContextualized(action)),
                this.executor, flags);
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

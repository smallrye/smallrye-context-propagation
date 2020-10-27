package io.smallrye.context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class Jdk12CompletableFutureWrapper<T> extends CompletableFutureWrapper<T> {

    public Jdk12CompletableFutureWrapper(SmallRyeThreadContext context, CompletableFuture<T> f, Executor executor,
                                         int flags) {
        super(context, f, executor, flags);
    }

    // Java 9
    
    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        CompletableFuture<U> ret = new CompletableFuture<>();
        return context.withContextCapture(ret, executor, flags);
    }

    @Override
    public Executor defaultExecutor() {
        return executor;
    }

    @Override
    public CompletableFuture<T> copy() {
        return context.withContextCapture(f.copy(), executor, flags);
    }

    @Override
    public CompletionStage<T> minimalCompletionStage() {
        // this creates a new MinimalStage we need to wrap
        return context.withContextCapture(f.minimalCompletionStage());
    }

    @Override
    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
        // just forward 
        return context.withContextCapture(f.completeAsync(context.contextualSupplierUnlessContextualized(supplier), executor),
                executor, flags);
    }

    @Override
    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
        // just forward 
        return context.withContextCapture(f.completeAsync(context.contextualSupplierUnlessContextualized(supplier), executor),
                this.executor, flags);
    }

    @Override
    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
        // just forward 
        return context.withContextCapture(f.orTimeout(timeout, unit), executor, flags);
    }

    @Override
    public CompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit) {
        // just forward 
        return context.withContextCapture(f.completeOnTimeout(value, timeout, unit), executor, flags);
    }

    // Java 12
    
    @Override
    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(f.exceptionallyAsync(context.contextualFunctionUnlessContextualized(fn), executor),
                                          executor, flags);
    }

    @Override
    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn, Executor executor) {
        return context.withContextCapture(f.exceptionallyAsync(context.contextualFunctionUnlessContextualized(fn), executor),
                                          this.executor, flags);
    }

    @Override
    public CompletableFuture<T> exceptionallyCompose(Function<Throwable, ? extends CompletionStage<T>> fn) {
        return context.withContextCapture(f.exceptionallyCompose(context.contextualFunctionUnlessContextualized(fn)),
                                          executor, flags);
    }

    @Override
    public CompletableFuture<T> exceptionallyComposeAsync(Function<Throwable, ? extends CompletionStage<T>> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(f.exceptionallyComposeAsync(context.contextualFunctionUnlessContextualized(fn), executor),
                                          executor, flags);
    }

    @Override
    public CompletableFuture<T> exceptionallyComposeAsync(Function<Throwable, ? extends CompletionStage<T>> fn, Executor executor) {
        return context.withContextCapture(f.exceptionallyComposeAsync(context.contextualFunctionUnlessContextualized(fn), executor),
                                          this.executor, flags);
    }
}

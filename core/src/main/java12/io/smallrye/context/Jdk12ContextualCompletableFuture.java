package io.smallrye.context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class Jdk12CompletableFutureWrapper<T> extends CompletableFutureWrapper<T> {

    public Jdk12CompletableFutureWrapper(SmallRyeThreadContext context, Executor executor,
                                         boolean minimal) {
        super(context, executor, minimal);
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

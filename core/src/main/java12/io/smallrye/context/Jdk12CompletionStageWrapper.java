package io.smallrye.context;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class Jdk12CompletionStageWrapper<T> extends CompletionStageWrapper<T> {

    public Jdk12CompletionStageWrapper(SmallRyeThreadContext context, CompletionStage<T> f, Executor executor) {
        super(context, f, executor);
    }

    // Java 12
    
    @Override
    public CompletionStage<T> exceptionallyAsync(Function<Throwable, ? extends T> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(f.exceptionallyAsync(context.contextualFunctionUnlessContextualized(fn), executor));
    }

    @Override
    public CompletionStage<T> exceptionallyAsync(Function<Throwable, ? extends T> fn, Executor executor) {
        return context.withContextCapture(f.exceptionallyAsync(context.contextualFunctionUnlessContextualized(fn), executor));
    }

    @Override
    public CompletionStage<T> exceptionallyCompose(Function<Throwable, ? extends CompletionStage<T>> fn) {
        return context.withContextCapture(f.exceptionallyCompose(context.contextualFunctionUnlessContextualized(fn)));
    }

    @Override
    public CompletionStage<T> exceptionallyComposeAsync(Function<Throwable, ? extends CompletionStage<T>> fn) {
        checkDefaultExecutor();
        return context.withContextCapture(f.exceptionallyComposeAsync(context.contextualFunctionUnlessContextualized(fn), executor));
    }

    @Override
    public CompletionStage<T> exceptionallyComposeAsync(Function<Throwable, ? extends CompletionStage<T>> fn, Executor executor) {
        return context.withContextCapture(f.exceptionallyComposeAsync(context.contextualFunctionUnlessContextualized(fn), executor));
    }
}

package io.smallrye.context.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import io.smallrye.context.CompletableFutureWrapper;
import io.smallrye.context.CompletionStageWrapper;
import io.smallrye.context.JdkSpecific;
import io.smallrye.context.SmallRyeThreadContext;

public class JdkSpecificImpl implements JdkSpecific.Contract {

    @Override
    public <T> CompletionStage<T> newCompletionStageWrapper(SmallRyeThreadContext threadContext,
            CompletionStage<T> future) {
        return new CompletionStageWrapper<>(threadContext, future);
    }

    @Override
    public <T> CompletableFuture<T> newCompletableFutureWrapper(SmallRyeThreadContext threadContext,
            CompletableFuture<T> future, Executor executor, boolean minimal) {
        return new CompletableFutureWrapper<>(threadContext, future, executor, minimal);
    }
}

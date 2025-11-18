package io.smallrye.context.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import io.smallrye.context.ContextualCompletableFuture;
import io.smallrye.context.JdkSpecific;
import io.smallrye.context.SmallRyeThreadContext;

public class JdkSpecificImpl implements JdkSpecific.Contract {

    @Override
    public <T> CompletionStage<T> newCompletionStage(SmallRyeThreadContext threadContext,
            Executor executor) {
        return new ContextualCompletableFuture<>(threadContext, executor, true);
    }

    @Override
    public <T> CompletableFuture<T> newCompletableFuture(SmallRyeThreadContext threadContext,
            Executor executor) {
        return new ContextualCompletableFuture<>(threadContext, executor, false);
    }
}

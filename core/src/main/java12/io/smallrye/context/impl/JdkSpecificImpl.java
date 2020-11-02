package io.smallrye.context.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import io.smallrye.context.Jdk12CompletableFutureWrapper;
import io.smallrye.context.Jdk12CompletionStageWrapper;
import io.smallrye.context.JdkSpecific;
import io.smallrye.context.SmallRyeThreadContext;

public class JdkSpecificImpl implements JdkSpecific.Contract {

    @Override
    public <T> CompletionStage<T> newCompletionStageWrapper(SmallRyeThreadContext threadContext,
            CompletionStage<T> future, Executor executor) {
        return new Jdk12CompletionStageWrapper<>(threadContext, future, executor);
    }

    @Override
    public <T> CompletableFuture<T> newCompletableFutureWrapper(SmallRyeThreadContext threadContext,
            CompletableFuture<T> future, Executor executor, int flags) {
        return new Jdk12CompletableFutureWrapper<>(threadContext, future, executor, flags);
    }

}

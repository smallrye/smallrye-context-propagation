package io.smallrye.context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import io.smallrye.context.impl.JdkSpecificImpl;

public class JdkSpecific {

    private final static JdkSpecificImpl impl = new JdkSpecificImpl();

    public interface Contract {
        public <T> CompletionStage<T> newCompletionStageWrapper(SmallRyeThreadContext threadContext,
                CompletionStage<T> future);

        public <T> CompletableFuture<T> newCompletableFutureWrapper(SmallRyeThreadContext threadContext,
                CompletableFuture<T> future, Executor executor, boolean minimal);
    }

    public static <T> CompletionStage<T> newCompletionStageWrapper(SmallRyeThreadContext threadContext,
            CompletionStage<T> future) {
        return impl.newCompletionStageWrapper(threadContext, future);
    }

    public static <T> CompletableFuture<T> newCompletableFutureWrapper(SmallRyeThreadContext threadContext,
            CompletableFuture<T> future, Executor executor, boolean minimal) {
        return impl.newCompletableFutureWrapper(threadContext, future, executor, minimal);
    }
}

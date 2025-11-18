package io.smallrye.context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import io.smallrye.context.impl.JdkSpecificImpl;

public class JdkSpecific {

    private final static JdkSpecificImpl impl = new JdkSpecificImpl();

    public interface Contract {
        public <T> CompletionStage<T> newCompletionStage(SmallRyeThreadContext threadContext,
                Executor executor);

        public <T> CompletableFuture<T> newCompletableFuture(SmallRyeThreadContext threadContext,
                Executor executor);
    }

    public static <T> CompletionStage<T> newCompletionStage(SmallRyeThreadContext threadContext,
            Executor executor) {
        return impl.newCompletionStage(threadContext, executor);
    }

    public static <T> CompletableFuture<T> newCompletableFuture(SmallRyeThreadContext threadContext,
            Executor executor) {
        return impl.newCompletableFuture(threadContext, executor);
    }
}

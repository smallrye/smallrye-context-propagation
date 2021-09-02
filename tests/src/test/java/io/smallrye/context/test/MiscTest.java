package io.smallrye.context.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.junit.Assert;
import org.junit.Test;

import io.smallrye.context.SmallRyeContextManager;
import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.test.util.AbstractTest;

public class MiscTest extends AbstractTest {

    @Test
    public void testCFWrapping() {
        ContextManager contextManager = ContextManagerProvider.instance().getContextManager();
        ThreadContext threadContext = contextManager.newThreadContextBuilder().build();

        CompletionStage<String> cs = CompletableFuture.completedFuture("foo");
        CompletionStage<String> wrapped = threadContext.withContextCapture(cs);
        Assert.assertTrue(wrapped instanceof CompletableFuture);
    }

    /**
     * Verify that dependent stages created via withContextCapture can be completed independently
     * of the original stage.
     * 
     * Modified from the spec to specify a default executor, which appears to make it fail.
     *
     * @throws ExecutionException indicates test failure
     * @throws InterruptedException indicates test failure
     */
    @Test
    public void withContextCaptureDependentStageForcedCompletion() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        SmallRyeContextManager contextManager = SmallRyeContextManagerProvider.instance().getContextManagerBuilder()
                .withDefaultExecutorService(executorService).build();
        ThreadContext contextPropagator = contextManager.newThreadContextBuilder()
                .propagated()
                .unchanged()
                .cleared(ThreadContext.ALL_REMAINING)
                .build();

        CompletableFuture<String> stage1 = new CompletableFuture<String>();
        CompletableFuture<String> stage2 = contextPropagator.withContextCapture(stage1);

        Assert.assertTrue(
                "It should be possible to complete a CompletableFuture created via withContextCapture without completing the original stage.",
                stage2.complete("stage_2_done"));

        Assert.assertFalse(
                "Completion of the dependent stage must not imply completion of the original stage.",
                stage1.isDone());

        Assert.assertTrue(
                "It should be possible to complete the original stage with a different result after dependent stage was forcibly completed.",
                stage1.complete("stage_1_done"));

        Assert.assertEquals(
                "Completion stage result does not match the result with which it was forcibly completed.",
                "stage_1_done",
                stage1.get());

        Assert.assertEquals(
                "Completion stage result does not match the result with which it was forcibly completed.",
                "stage_2_done",
                stage2.get());

        executorService.shutdown();
    }

}

package io.smallrye.context.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.junit.jupiter.api.Test;

import io.smallrye.context.SmallRyeContextManager;
import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.test.util.AbstractTest;

class MiscTest extends AbstractTest {

    @Test
    void testCFWrapping() {
        ContextManager contextManager = ContextManagerProvider.instance().getContextManager();
        ThreadContext threadContext = contextManager.newThreadContextBuilder().build();

        CompletionStage<String> cs = CompletableFuture.completedFuture("foo");
        CompletionStage<String> wrapped = threadContext.withContextCapture(cs);
        assertTrue(wrapped instanceof CompletableFuture);
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
    void contextCaptureDependentStageForcedCompletion() throws ExecutionException, InterruptedException {
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

        assertTrue(stage2.complete("stage_2_done"),
                "It should be possible to complete a CompletableFuture created via withContextCapture without completing the original stage.");

        assertFalse(stage1.isDone(), "Completion of the dependent stage must not imply completion of the original stage.");

        assertTrue(stage1.complete("stage_1_done"),
                "It should be possible to complete the original stage with a different result after dependent stage was forcibly completed.");

        assertEquals("stage_1_done", stage1.get(),
                "Completion stage result does not match the result with which it was forcibly completed.");

        assertEquals("stage_2_done", stage2.get(),
                "Completion stage result does not match the result with which it was forcibly completed.");

        executorService.shutdown();
    }

    @Test
    public void issue444() {
        CompletableFuture<String> cs = new CompletableFuture<String>();

        CompletableFuture<String> waitFor1 = cs.whenComplete((result, error) -> {
            assertEquals(true, cs.isDone());
        });

        cs.complete("something");
        waitFor1.join();

        CompletableFuture<String> cs2 = new CompletableFuture<String>();
        CompletableFuture<String> csw = SmallRyeThreadContext.getCurrentThreadContextOrDefaultContexts()
                .withContextCapture(cs2);

        CompletableFuture<String> waitFor2 = csw.whenComplete((result, error) -> {
            assertEquals(true, csw.isDone());
        });

        cs2.complete("something");
        waitFor2.join();
    }
}

package io.smallrye.context.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.SmallRyeThreadContext;

public class ManualPropagationMultipleRequestTest {

    private static SmallRyeThreadContext threadContext;
    private static SmallRyeThreadContext minimalThreadContext;

    @BeforeClass
    public static void init() {
        threadContext = (SmallRyeThreadContext) ContextManagerProvider.instance().getContextManager().newThreadContextBuilder()
                .build();
        minimalThreadContext = SmallRyeContextManagerProvider.instance().getContextManagerBuilder()
                .withThreadContextProviders(new MyThreadContextProvider())
                .build()
                .newThreadContextBuilder()
                .propagated(MyThreadContextProvider.MY_CONTEXT_TYPE)
                .unchanged(ThreadContext.ALL_REMAINING)
                .build();
    }

    public void newRequest(String reqId) {
        // seed
        MyContext.init();

        MyContext.get().set(reqId);
    }

    public void endOfRequest() {
        MyContext.clear();
    }

    @Test
    public void testRunnableOnSingleWorkerThread() throws Throwable {
        testRunnable(Executors.newFixedThreadPool(1));
    }

    @Test
    public void testRunnableOnTwoWorkerThread() throws Throwable {
        testRunnable(Executors.newFixedThreadPool(2));
    }

    private void testRunnable(ExecutorService executor) throws Throwable {
        newRequest("req 1");
        Future<?> task1 = executor.submit(threadContext.contextualRunnable(() -> {
            checkContextCaptured("req 1");
            endOfRequest();
        }));

        newRequest("req 2");
        Future<?> task2 = executor.submit(threadContext.contextualRunnable(() -> {
            checkContextCaptured("req 2");
            endOfRequest();
        }));

        task1.get();
        task2.get();
        executor.shutdown();
    }

    @Test
    public void testFastRunnableOnSingleWorkerThread() throws Throwable {
        testFastRunnable(Executors.newFixedThreadPool(1));
    }

    @Test
    public void testFastRunnableOnTwoWorkerThread() throws Throwable {
        testFastRunnable(Executors.newFixedThreadPool(2));
    }

    private void testFastRunnable(ExecutorService executor) throws Throwable {
        newRequest("req 1");
        Future<?> task1 = executor.submit(minimalThreadContext.contextualRunnable(() -> {
            checkContextCaptured("req 1");
            endOfRequest();
        }));

        newRequest("req 2");
        Future<?> task2 = executor.submit(minimalThreadContext.contextualRunnable(() -> {
            checkContextCaptured("req 2");
            endOfRequest();
        }));

        task1.get();
        task2.get();
        executor.shutdown();
    }

    @Test
    public void testCompletionStageOnSingleWorkerThread() throws Throwable {
        ManagedExecutor executor = ContextManagerProvider.instance().getContextManager().newManagedExecutorBuilder().maxAsync(1)
                .build();
        testCompletionStage(executor);
    }

    @Test
    public void testCompletionStageOnTwoWorkerThread() throws Throwable {
        ManagedExecutor executor = ContextManagerProvider.instance().getContextManager().newManagedExecutorBuilder().maxAsync(2)
                .build();
        testCompletionStage(executor);
    }

    private void testCompletionStage(ManagedExecutor executor) throws Throwable {
        CountDownLatch latch = new CountDownLatch(2);

        Throwable[] ret = new Throwable[2];

        newRequest("req 1");
        CompletableFuture<Void> cf1 = executor.newIncompleteFuture();
        cf1.handleAsync((v, t) -> {
            try {
                ret[0] = t;
                checkContextCaptured("req 1");
                endOfRequest();
            } catch (Throwable t2) {
                ret[0] = t2;
            }
            latch.countDown();
            return null;
        });

        newRequest("req 2");
        CompletableFuture<Void> cf2 = executor.newIncompleteFuture();
        cf2.handleAsync((v, t) -> {
            try {
                ret[1] = t;
                checkContextCaptured("req 2");
                endOfRequest();
            } catch (Throwable t2) {
                ret[1] = t2;
            }
            latch.countDown();
            return null;
        });

        cf1.complete(null);
        cf2.complete(null);
        latch.await();
        if (ret[0] != null)
            throw ret[0];
        if (ret[1] != null)
            throw ret[1];
        executor.shutdown();
    }

    private void checkContextCaptured(String reqId) {
        Assert.assertEquals(reqId, MyContext.get().getReqId());
    }
}

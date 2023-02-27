/*
 * Copyright (c) 2018,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.context.tck;

import java.io.CharConversionException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.tck.contexts.buffer.Buffer;
import org.eclipse.microprofile.context.tck.contexts.buffer.spi.BufferContextProvider;
import org.eclipse.microprofile.context.tck.contexts.label.Label;
import org.eclipse.microprofile.context.tck.contexts.label.spi.LabelContextProvider;
import org.eclipse.microprofile.context.tck.contexts.priority.spi.ThreadPriorityContextProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.smallrye.context.SmallRyeContextManager;
import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.SmallRyeThreadContext;

/**
 * Copied from the TCK in order to test the CS/CF methods on ThreadContext
 */
public class ThreadContextCSCFTest extends Arquillian {
    /**
     * Maximum tolerated wait for an asynchronous operation to complete.
     * This is important to ensure that tests don't hang waiting for asynchronous operations to complete.
     * Normally these sort of operations will complete in tiny fractions of a second, but we are specifying
     * an extremely generous value here to allow for the widest possible variety of test execution environments.
     */
    private static final long MAX_WAIT_NS = TimeUnit.MINUTES.toNanos(2);

    /**
     * Pool of unmanaged threads (not context-aware) that can be used by tests.
     */
    private ExecutorService unmanagedThreads;

    @AfterClass
    public void after() {
        unmanagedThreads.shutdownNow();
    }

    @AfterMethod
    public void afterMethod(Method m, ITestResult result) {
        System.out.println(
                "<<< END " + m.getClass().getSimpleName() + '.' + m.getName() + (result.isSuccess() ? " SUCCESS" : " FAILED"));
        Throwable failure = result.getThrowable();
        if (failure != null) {
            failure.printStackTrace(System.out);
        }
    }

    @BeforeClass
    public void before() {
        unmanagedThreads = Executors.newFixedThreadPool(5);
    }

    @BeforeMethod
    public void beforeMethod(Method m) {
        System.out.println(">>> BEGIN " + m.getClass().getSimpleName() + '.' + m.getName());
    }

    @Deployment
    public static WebArchive createDeployment() {
        // build a JAR that provides three fake context types: 'Buffer', 'Label', and 'ThreadPriority'
        JavaArchive fakeContextProviders = ShrinkWrap.create(JavaArchive.class, "fakeContextTypes.jar")
                .addPackages(true, "org.eclipse.microprofile.context.tck.contexts.buffer")
                .addPackages(true, "org.eclipse.microprofile.context.tck.contexts.label")
                .addPackage("org.eclipse.microprofile.context.tck.contexts.priority.spi")
                .addAsServiceProvider(ThreadContextProvider.class,
                        BufferContextProvider.class, LabelContextProvider.class, ThreadPriorityContextProvider.class);

        return ShrinkWrap.create(WebArchive.class, ThreadContextCSCFTest.class.getSimpleName() + ".war")
                .addClasses(ThreadContextCSCFTest.class)
                .addAsLibraries(fakeContextProviders);
    }

    /**
     * Verify that the ManagedExecutor implementation clears context
     * types that are not configured under propagated, or cleared.
     *
     * @throws TimeoutException indicates test failure
     * @throws ExecutionException indicates test failure
     * @throws InterruptedException indicates test failure
     */
    @Test
    public void clearUnspecifiedContexts() throws InterruptedException, ExecutionException, TimeoutException {
        SmallRyeThreadContext executor = SmallRyeThreadContext.builder()
                .propagated(Buffer.CONTEXT_NAME)
                .build();

        int originalPriority = Thread.currentThread().getPriority();
        try {
            // Set non-default values
            int newPriority = originalPriority == 3 ? 2 : 3;
            Thread.currentThread().setPriority(newPriority);
            Buffer.set(new StringBuffer("clearUnspecifiedContexts-test-buffer-A"));

            Future<Void> future = executor.completedFuture(1).thenRun(() -> {
                Assert.assertEquals(Buffer.get().toString(), "clearUnspecifiedContexts-test-buffer-A",
                        "Context type was not propagated to contextual action.");

                Buffer.set(new StringBuffer("clearUnspecifiedContexts-test-buffer-B"));

                Assert.assertEquals(Thread.currentThread().getPriority(), Thread.NORM_PRIORITY,
                        "Context type that remained unspecified was not cleared by default.");
            });

            Assert.assertNull(future.get(MAX_WAIT_NS, TimeUnit.NANOSECONDS),
                    "Non-null value returned by stage that runs Runnable.");

            Assert.assertEquals(Buffer.get().toString(), "clearUnspecifiedContexts-test-buffer-A",
                    "Previous context (Buffer) was not restored after context was propagated for contextual action.");
        } finally {
            // Restore original values
            Buffer.set(null);
            Thread.currentThread().setPriority(originalPriority);
        }
    }

    /**
     * Verify that thread context is captured and propagated per the configuration of the
     * ManagedExecutor builder for all dependent stages of the completed future that is created
     * by the ManagedExecutor's completedFuture implementation. Thread context is captured
     * at each point where a dependent stage is added, rather than solely upon creation of the
     * initial stage or construction of the builder.
     *
     * @throws ExecutionException indicates test failure
     * @throws InterruptedException indicates test failure
     * @throws TimeoutException indicates test failure
     */
    @Test
    public void completedFutureDependentStagesRunWithContext()
            throws ExecutionException, InterruptedException, TimeoutException {
        SmallRyeThreadContext executor = SmallRyeThreadContext.builder()
                .propagated(Buffer.CONTEXT_NAME)
                .cleared(ThreadContext.ALL_REMAINING)
                .build();

        try {
            // Set non-default values
            Buffer.set(new StringBuffer("completedFuture-test-buffer-A"));
            Label.set("completedFuture-test-label");

            CompletableFuture<Long> stage1a = executor.completedFuture(1000L);

            Assert.assertTrue(stage1a.isDone(),
                    "Future created by completedFuture is not complete.");

            Assert.assertFalse(stage1a.isCompletedExceptionally(),
                    "Future created by completedFuture reports exceptional completion.");

            Assert.assertEquals(stage1a.getNow(1234L), Long.valueOf(1000L),
                    "Future created by completedFuture has result that differs from what was specified.");

            // The following incomplete future blocks subsequent stages from running inline on the current thread
            CompletableFuture<Long> stage1b = new CompletableFuture<Long>();

            Buffer.set(new StringBuffer("completedFuture-test-buffer-B"));

            CompletableFuture<Long> stage2 = stage1a.thenCombine(stage1b, (a, b) -> {
                Assert.assertEquals(a, Long.valueOf(1000L),
                        "First value supplied to BiFunction was lost or altered.");

                Assert.assertEquals(b, Long.valueOf(3L),
                        "Second value supplied to BiFunction was lost or altered.");

                Assert.assertEquals(Buffer.get().toString(), "completedFuture-test-buffer-B",
                        "Context type was not propagated to contextual action.");

                Assert.assertEquals(Label.get(), "",
                        "Context type that is configured to be cleared was not cleared.");

                return a * b;
            });

            Buffer.set(new StringBuffer("completedFuture-test-buffer-C"));

            CompletableFuture<Long> stage3 = stage2.thenApply(i -> {
                Assert.assertEquals(i, Long.valueOf(3000L),
                        "Value supplied to third stage was lost or altered.");

                Assert.assertEquals(Buffer.get().toString(), "completedFuture-test-buffer-C",
                        "Context type was not propagated to contextual action.");

                // This stage runs inline on the same thread as the test, so alter the
                // context here and later verify that the MicroProfile Context Propagation implementation
                // properly restores it to the thread's previous value, which will be
                // completedFuture-test-buffer-E at the point when this runs.
                Buffer.set(new StringBuffer("completedFuture-test-buffer-D"));

                Assert.assertEquals(Label.get(), "",
                        "Context type that is configured to be cleared was not cleared.");

                return i - 300;
            });

            Buffer.set(new StringBuffer("completedFuture-test-buffer-E"));

            // Complete stage 1b, allowing stage 2 and then 3 to run
            stage1b.complete(3L);

            Assert.assertEquals(stage3.get(MAX_WAIT_NS, TimeUnit.NANOSECONDS), Long.valueOf(2700L),
                    "Unexpected result for stage 3.");

            Assert.assertEquals(stage2.getNow(3333L), Long.valueOf(3000L),
                    "Unexpected or missing result for stage 2.");

            Assert.assertTrue(stage2.isDone(), "Second stage did not transition to done upon completion.");
            Assert.assertTrue(stage3.isDone(), "Third stage did not transition to done upon completion.");

            Assert.assertFalse(stage2.isCompletedExceptionally(), "Second stage should not report exceptional completion.");
            Assert.assertFalse(stage3.isCompletedExceptionally(), "Third stage should not report exceptional completion.");

            // Is context properly restored on current thread?
            Assert.assertEquals(Buffer.get().toString(), "completedFuture-test-buffer-E",
                    "Previous context was not restored after context was cleared for managed executor tasks.");
            Assert.assertEquals(Label.get(), "completedFuture-test-label",
                    "Previous context was not restored after context was propagated for managed executor tasks.");
        } finally {
            // Restore original values
            Buffer.set(null);
            Label.set(null);
        }
    }

    /**
     * Verify that thread context is captured and propagated per the configuration of the
     * ManagedExecutor builder for all dependent stages of the completed future that is created
     * by the ManagedExecutor's completedStage implementation. Thread context is captured
     * at each point where a dependent stage is added, rather than solely upon creation of the
     * initial stage or construction of the builder.
     *
     * @throws InterruptedException indicates test failure
     */
    @Test
    public void completedStageDependentStagesRunWithContext() throws InterruptedException {
        SmallRyeThreadContext executor = SmallRyeThreadContext.builder()
                .propagated(Label.CONTEXT_NAME)
                .cleared(ThreadContext.ALL_REMAINING)
                .build();

        try {
            // Set non-default values
            Buffer.set(new StringBuffer("completedStage-test-buffer"));
            Label.set("completedStage-test-label-A");

            CompletionStage<String> stage1 = executor.completedStage("5A");

            // The following incomplete future prevents subsequent stages from completing
            CompletableFuture<Integer> stage2 = new CompletableFuture<Integer>();

            Label.set("completedStage-test-label-B");

            CompletionStage<Integer> stage3 = stage1.thenCompose(s -> {
                Assert.assertEquals(s, "5A",
                        "Value supplied to compose function was lost or altered.");

                Assert.assertEquals(Buffer.get().toString(), "",
                        "Context type that is configured to be cleared was not cleared.");

                Assert.assertEquals(Label.get(), "completedStage-test-label-B",
                        "Context type was not propagated to contextual action.");

                return stage2.thenApply(i -> i + Integer.parseInt(s, 16));
            });

            Label.set("completedStage-test-label-C");

            CompletionStage<Integer> stage4 = stage3.applyToEither(new CompletableFuture<Integer>(), i -> {
                Assert.assertEquals(i, Integer.valueOf(99),
                        "Value supplied to function was lost or altered.");

                Assert.assertEquals(Buffer.get().toString(), "",
                        "Context type that is configured to be cleared was not cleared.");

                Assert.assertEquals(Label.get(), "completedStage-test-label-C",
                        "Context type was not propagated to contextual action.");

                return i + 1;
            });

            Label.set("completedStage-test-label-D");

            CountDownLatch completed = new CountDownLatch(1);
            AtomicInteger resultRef = new AtomicInteger();
            stage4.whenComplete((result, failure) -> {
                resultRef.set(result);
                completed.countDown();
            });

            // allow stages 3 and 4 to complete
            stage2.complete(9);

            Assert.assertTrue(completed.await(MAX_WAIT_NS, TimeUnit.NANOSECONDS),
                    "Completion stage did not finish in a reasonable amount of time.");

            Assert.assertEquals(resultRef.get(), 100,
                    "Unexpected result for stage 4.");

            // Is context properly restored on current thread?
            Assert.assertEquals(Buffer.get().toString(), "completedStage-test-buffer",
                    "Previous context was not restored after context was cleared for managed executor tasks.");
            Assert.assertEquals(Label.get(), "completedStage-test-label-D",
                    "Previous context was not restored after context was propagated for managed executor tasks.");
        } finally {
            // Restore original values
            Buffer.set(null);
            Label.set(null);
        }
    }

    /**
     * When an already-contextualized Function is specified as the action/task,
     * the action/task runs with its already-captured context rather than
     * capturing and applying context per the configuration of the managed executor.
     *
     * @throws ExecutionException indicates test failure
     * @throws InterruptedException indicates test failure
     * @throws TimeoutException indicates test failure
     */
    @Test
    public void contextOfContextualFunctionOverridesContextOfManagedExecutor()
            throws ExecutionException, InterruptedException, TimeoutException {
        SmallRyeContextManager smallRyeContextManager = SmallRyeContextManagerProvider
                .instance()
                .getContextManagerBuilder()
                .addDiscoveredThreadContextProviders()
                .withDefaultExecutorService(unmanagedThreads)
                .build();
        ThreadContext labelContext = smallRyeContextManager.newThreadContextBuilder()
                .propagated(Label.CONTEXT_NAME)
                .unchanged()
                .cleared(ThreadContext.ALL_REMAINING)
                .build();

        SmallRyeThreadContext executor = smallRyeContextManager.newThreadContextBuilder()
                .propagated(Buffer.CONTEXT_NAME)
                .cleared(ThreadContext.ALL_REMAINING)
                .build();
        try {
            Buffer.set(new StringBuffer("contextualFunctionOverride-buffer-1"));
            Label.set("contextualFunctionOverride-label-1");

            Function<Integer, Integer> precontextualizedFunction1 = labelContext.contextualFunction(i -> {
                Assert.assertEquals(Label.get(), "contextualFunctionOverride-label-1",
                        "Previously captured context type not found on thread.");
                Assert.assertEquals(Buffer.get().toString(), "",
                        "Context type not cleared from thread.");
                return i + 1;
            });

            Buffer.set(new StringBuffer("contextualFunctionOverride-buffer-2"));
            Label.set("contextualFunctionOverride-label-2");

            Function<Integer, Integer> precontextualizedFunction2 = labelContext.contextualFunction(i -> {
                Assert.assertEquals(Label.get(), "contextualFunctionOverride-label-2",
                        "Previously captured context type not found on thread.");
                Assert.assertEquals(Buffer.get().toString(), "",
                        "Context type not cleared from thread.");
                return i + 20;
            });

            Function<Throwable, Integer> precontextualizedErrorHandler = labelContext.contextualFunction(failure -> {
                Assert.assertEquals(Label.get(), "contextualFunctionOverride-label-2",
                        "Previously captured context type not found on thread.");
                Assert.assertEquals(Buffer.get().toString(), "",
                        "Context type not cleared from thread.");
                return -1;
            });

            Buffer.set(new StringBuffer("contextualFunctionOverride-buffer-3"));
            Label.set("contextualFunctionOverride-label-3");

            Function<Integer, Integer> normalFunction = i -> {
                Assert.assertEquals(Buffer.get().toString(), "contextualFunctionOverride-buffer-3",
                        "Previously captured context type not found on thread.");
                Assert.assertEquals(Label.get(), "",
                        "Context type not cleared from thread.");
                return i + 300;
            };

            CompletableFuture<Integer> stage0 = executor.newIncompleteFuture();
            CompletableFuture<Integer> stage1 = stage0.thenApplyAsync(precontextualizedFunction1);

            Buffer.set(new StringBuffer("contextualFunctionOverride-buffer-4"));
            Label.set("contextualFunctionOverride-label-4");

            Function<Integer, CompletableFuture<Integer>> precontextualizedFunction4 = labelContext.contextualFunction(i -> {
                Assert.assertEquals(Label.get(), "contextualFunctionOverride-label-4",
                        "Previously captured context type not found on thread.");
                Assert.assertEquals(Buffer.get().toString(), "",
                        "Context type not cleared from thread.");
                return stage1;
            });

            Buffer.set(new StringBuffer("contextualFunctionOverride-buffer-3"));
            Label.set("contextualFunctionOverride-label-3");

            CompletableFuture<Integer> stage2 = stage0.thenComposeAsync(precontextualizedFunction4);
            CompletableFuture<Integer> stage3 = stage2.applyToEither(stage1, precontextualizedFunction2);
            CompletableFuture<Integer> stage4 = stage3.thenApply(normalFunction);
            CompletableFuture<Integer> stage5 = stage4.thenApply(i -> i / (i - 321)) // intentional ArithmeticException for division by 0
                    .exceptionally(precontextualizedErrorHandler);

            stage0.complete(0);

            Assert.assertEquals(stage2.join(), Integer.valueOf(1),
                    "Unexpected result for completion stage.");

            Assert.assertEquals(stage5.join(), Integer.valueOf(-1),
                    "Unexpected result for completion stage.");
        } finally {
            // Restore original values
            Buffer.set(null);
            Label.set(null);
        }
    }

    /**
     * Verify that thread context is captured and propagated per the configuration of the
     * ManagedExecutor builder for all dependent stages of the completed future that is created
     * by the ManagedExecutor's failedFuture implementation. Thread context is captured
     * at each point where a dependent stage is added, rather than solely upon creation of the
     * initial stage or construction of the builder.
     *
     * @throws ExecutionException indicates test failure
     * @throws InterruptedException indicates test failure
     * @throws TimeoutException indicates test failure
     */
    @Test
    public void failedFutureDependentStagesRunWithContext() throws ExecutionException, InterruptedException, TimeoutException {
        SmallRyeThreadContext executor = SmallRyeThreadContext.builder()
                .propagated(Buffer.CONTEXT_NAME)
                .cleared(ThreadContext.ALL_REMAINING)
                .build();

        try {
            // Set non-default values
            Buffer.set(new StringBuffer("failedFuture-test-buffer-1"));
            Label.set("failedFuture-test-label");

            CompletableFuture<Character> stage1 = executor
                    .failedFuture(new CharConversionException("A fake exception created by the test"));

            Assert.assertTrue(stage1.isDone(),
                    "Future created by failedFuture is not complete.");

            Assert.assertTrue(stage1.isCompletedExceptionally(),
                    "Future created by failedFuture does not report exceptional completion.");

            try {
                Character result = stage1.getNow('1');
                Assert.fail("Failed future must raise exception. Instead, getNow returned: " + result);
            } catch (CompletionException x) {
                if (x.getCause() == null || !(x.getCause() instanceof CharConversionException)
                        || !"A fake exception created by the test".equals(x.getCause().getMessage())) {
                    throw x;
                }
            }

            Buffer.set(new StringBuffer("failedFuture-test-buffer-B"));

            CompletableFuture<Character> stage2a = stage1.exceptionally(x -> {
                Assert.assertEquals(x.getClass(), CharConversionException.class,
                        "Wrong exception class supplied to 'exceptionally' method.");

                Assert.assertEquals(x.getMessage(), "A fake exception created by the test",
                        "Exception message was lost or altered.");

                Assert.assertEquals(Buffer.get().toString(), "failedFuture-test-buffer-B",
                        "Context type was not propagated to contextual action.");

                Assert.assertEquals(Label.get(), "",
                        "Context type that is configured to be cleared was not cleared.");

                return 'A';
            });

            // The following incomplete future blocks subsequent stages from running inline on the current thread
            CompletableFuture<Character> stage2b = new CompletableFuture<Character>();

            Buffer.set(new StringBuffer("failedFuture-test-buffer-C"));

            AtomicBoolean stage3Runs = new AtomicBoolean();

            CompletableFuture<Void> stage3 = stage2a.runAfterBoth(stage2b, () -> {
                stage3Runs.set(true);

                Assert.assertEquals(Buffer.get().toString(), "failedFuture-test-buffer-C",
                        "Context type was not propagated to contextual action.");

                Assert.assertEquals(Label.get(), "",
                        "Context type that is configured to be cleared was not cleared.");
            });

            Buffer.set(new StringBuffer("failedFuture-test-buffer-D"));

            Assert.assertFalse(stage3.isDone(),
                    "Third stage should not report done until both of the stages upon which it depends complete.");

            // Complete stage 2b, allowing stage 3 to run
            stage2b.complete('B');

            Assert.assertNull(stage3.get(MAX_WAIT_NS, TimeUnit.NANOSECONDS),
                    "Unexpected result for stage 3.");

            Assert.assertTrue(stage3Runs.get(),
                    "The Runnable for stage 3 did not run.");

            Assert.assertEquals(stage2a.getNow('F'), Character.valueOf('A'),
                    "Unexpected or missing result for stage 2.");

            Assert.assertTrue(stage2a.isDone(), "Second stage did not transition to done upon completion.");
            Assert.assertTrue(stage3.isDone(), "Third stage did not transition to done upon completion.");

            Assert.assertFalse(stage2a.isCompletedExceptionally(), "Second stage should not report exceptional completion.");
            Assert.assertFalse(stage3.isCompletedExceptionally(), "Third stage should not report exceptional completion.");

            // Is context properly restored on current thread?
            Assert.assertEquals(Buffer.get().toString(), "failedFuture-test-buffer-D",
                    "Previous context was not restored after context was cleared for managed executor tasks.");
            Assert.assertEquals(Label.get(), "failedFuture-test-label",
                    "Previous context was not restored after context was propagated for managed executor tasks.");
        } finally {
            // Restore original values
            Buffer.set(null);
            Label.set(null);
        }
    }

    /**
     * Verify that thread context is captured and propagated per the configuration of the
     * ManagedExecutor builder for all dependent stages of the completed future that is created
     * by the ManagedExecutor's failedStage implementation. Thread context is captured
     * at each point where a dependent stage is added, rather than solely upon creation of the
     * initial stage or construction of the builder.
     *
     * @throws ExecutionException indicates test failure
     * @throws InterruptedException indicates test failure
     * @throws TimeoutException indicates test failure
     */
    @Test
    public void failedStageDependentStagesRunWithContext() throws ExecutionException, InterruptedException, TimeoutException {
        SmallRyeThreadContext executor = SmallRyeThreadContext.builder()
                .propagated(Label.CONTEXT_NAME)
                .cleared(ThreadContext.ALL_REMAINING)
                .build();

        try {
            // Set non-default values
            Buffer.set(new StringBuffer("failedStage-test-buffer"));
            Label.set("failedStage-test-label-A");

            CompletionStage<Integer> stage1 = executor.failedStage(new LinkageError("Error intentionally raised by test case"));

            Label.set("failedStage-test-label-B");

            CompletionStage<Integer> stage2 = stage1.whenComplete((result, failure) -> {
                Assert.assertEquals(failure.getClass(), LinkageError.class,
                        "Wrong exception class supplied to 'whenComplete' method.");

                Assert.assertEquals(failure.getMessage(), "Error intentionally raised by test case",
                        "Error message was lost or altered.");

                Assert.assertNull(result,
                        "Non-null result supplied to whenComplete for failed stage.");

                Assert.assertEquals(Buffer.get().toString(), "",
                        "Context type that is configured to be cleared was not cleared.");

                Assert.assertEquals(Label.get(), "failedStage-test-label-B",
                        "Context type was not propagated to contextual action.");

            });

            Label.set("failedStage-test-label-C");

            CompletableFuture<Integer> future1 = stage1.toCompletableFuture();
            try {
                Integer result = future1.join();
                Assert.fail("The join operation did not raise the error from the failed stage. Instead: " + result);
            } catch (CompletionException x) {
                if (x.getCause() == null || !(x.getCause() instanceof LinkageError)
                        || !"Error intentionally raised by test case".equals(x.getCause().getMessage())) {
                    throw x;
                }
            }

            CompletableFuture<Integer> future2 = stage2.toCompletableFuture();
            try {
                Integer result = future2.get();
                Assert.fail("The get operation did not raise the error from the failed stage. Instead: " + result);
            } catch (ExecutionException x) {
                if (x.getCause() == null || !(x.getCause() instanceof LinkageError)
                        || !"Error intentionally raised by test case".equals(x.getCause().getMessage())) {
                    throw x;
                }
            }

            Assert.assertEquals(Buffer.get().toString(), "failedStage-test-buffer",
                    "Previous context was not restored after context was cleared for managed executor tasks.");
            Assert.assertEquals(Label.get(), "failedStage-test-label-C",
                    "Previous context was not restored after context was propagated for managed executor tasks.");
        } finally {
            // Restore original values
            Buffer.set(null);
            Label.set(null);
        }
    }

    /**
     * Verify that thread context is captured and propagated per the configuration of the
     * ManagedExecutor builder for all dependent stages of the incomplete future that is created
     * by the ManagedExecutor's newIncompleteFuture implementation. Thread context is captured
     * at each point where a dependent stage is added, rather than solely upon creation of the
     * initial stage or construction of the builder.
     *
     * @throws ExecutionException indicates test failure
     * @throws InterruptedException indicates test failure
     */
    @Test
    public void newIncompleteFutureDependentStagesRunWithContext() throws ExecutionException, InterruptedException {
        SmallRyeThreadContext executor = SmallRyeThreadContext.builder()
                .propagated(Label.CONTEXT_NAME)
                .cleared(ThreadContext.ALL_REMAINING)
                .build();

        try {
            CompletableFuture<Integer> stage1 = executor.newIncompleteFuture();

            Assert.assertFalse(stage1.isDone(),
                    "Completable future created by newIncompleteFuture did not start out as incomplete.");

            // Set non-default values
            Buffer.get().append("newIncompleteFuture-test-buffer");
            Label.set("newIncompleteFuture-test-label-A");

            CompletableFuture<Integer> stage2 = stage1.thenApply(i -> {
                Assert.assertEquals(i, Integer.valueOf(10),
                        "Value supplied to second stage was lost or altered.");

                Assert.assertEquals(Buffer.get().toString(), "",
                        "Context type that is configured to be cleared was not cleared.");

                Assert.assertEquals(Label.get(), "newIncompleteFuture-test-label-A",
                        "Context type was not correctly propagated to contextual action.");

                return i * 2;
            });

            Label.set("newIncompleteFuture-test-label-B");

            CompletableFuture<Integer> stage3 = stage2.thenApply(i -> {
                Assert.assertEquals(i, Integer.valueOf(20),
                        "Value supplied to third stage was lost or altered.");

                Assert.assertEquals(Buffer.get().toString(), "",
                        "Context type that is configured to be cleared was not cleared.");

                Assert.assertEquals(Label.get(), "newIncompleteFuture-test-label-B",
                        "Context type was not correctly propagated to contextual action.");

                return i + 10;
            });

            Label.set("newIncompleteFuture-test-label-C");

            // To avoid the possibility that CompletableFuture.get might cause the action to run
            // on the current thread, which would bypass the intent of testing context propagation,
            // use a countdown latch to independently wait for completion.
            CountDownLatch completed = new CountDownLatch(1);
            stage3.whenComplete((result, failure) -> completed.countDown());

            Assert.assertTrue(stage1.complete(10),
                    "Unable to complete the future that was created by newIncompleteFuture.");

            Assert.assertTrue(completed.await(MAX_WAIT_NS, TimeUnit.NANOSECONDS),
                    "Completable future did not finish in a reasonable amount of time.");

            Assert.assertTrue(stage1.isDone(), "First stage did not transition to done upon completion.");
            Assert.assertTrue(stage2.isDone(), "Second stage did not transition to done upon completion.");
            Assert.assertTrue(stage3.isDone(), "Third stage did not transition to done upon completion.");

            Assert.assertEquals(stage1.get(), Integer.valueOf(10),
                    "Result of first stage does not match the value with which it was completed.");

            Assert.assertEquals(stage2.getNow(22), Integer.valueOf(20),
                    "Result of second stage was lost or altered.");

            Assert.assertEquals(stage3.join(), Integer.valueOf(30),
                    "Result of third stage was lost or altered.");

            Assert.assertFalse(stage1.isCompletedExceptionally(), "First stage should not report exceptional completion.");
            Assert.assertFalse(stage2.isCompletedExceptionally(), "Second stage should not report exceptional completion.");
            Assert.assertFalse(stage3.isCompletedExceptionally(), "Third stage should not report exceptional completion.");

            // Is context properly restored on current thread?
            Assert.assertEquals(Buffer.get().toString(), "newIncompleteFuture-test-buffer",
                    "Previous context was not restored after context was cleared for managed executor tasks.");
            Assert.assertEquals(Label.get(), "newIncompleteFuture-test-label-C",
                    "Previous context was not restored after context was propagated for managed executor tasks.");
        } finally {
            // Restore original values
            Buffer.set(null);
            Label.set(null);
        }
    }

    /**
     * Verify that we can copy a CompletableFuture and get context propagation in the copy's dependent stages.
     *
     * @throws InterruptedException indicates test failure
     * @throws ExecutionException indicates test failure
     * @throws TimeoutException indicates test failure
     */
    @Test
    public void copyCompletableFuture() throws InterruptedException, ExecutionException, TimeoutException {
        SmallRyeContextManager smallRyeContextManager = SmallRyeContextManagerProvider
                .instance()
                .getContextManagerBuilder()
                .addDiscoveredThreadContextProviders()
                .withDefaultExecutorService(unmanagedThreads)
                .build();
        SmallRyeThreadContext executor = smallRyeContextManager.newThreadContextBuilder()
                .propagated(Label.CONTEXT_NAME)
                .cleared(ThreadContext.ALL_REMAINING)
                .build();

        try {
            // Set non-default values
            Label.set("copy-test-label-A");

            CompletableFuture<String> noContextCF = new CompletableFuture<>();
            CompletableFuture<String> noContextCFStage1 = noContextCF.thenApplyAsync(res -> {
                Assert.assertEquals(Label.get(), "",
                        "Context type should not be propagated to non-contextual action.");

                return res;
            });

            CompletableFuture<String> contextCF = executor.copy(noContextCFStage1).thenApplyAsync(res -> {
                Assert.assertEquals(Label.get(), "copy-test-label-A",
                        "Context type should be propagated to contextual action.");

                return res;
            });

            noContextCF.complete("OK");
            contextCF.get(MAX_WAIT_NS, TimeUnit.NANOSECONDS);
        } finally {
            // Restore original values
            Label.set(null);
        }
    }

    /**
     * Verify that we can copy a CompletionStage and get context propagation in the copy's dependent stages.
     *
     * @throws InterruptedException indicates test failure
     * @throws ExecutionException indicates test failure
     * @throws TimeoutException indicates test failure
     */
    @Test
    public void copyCompletionStage() throws InterruptedException, ExecutionException, TimeoutException {
        SmallRyeContextManager smallRyeContextManager = SmallRyeContextManagerProvider
                .instance()
                .getContextManagerBuilder()
                .addDiscoveredThreadContextProviders()
                .withDefaultExecutorService(unmanagedThreads)
                .build();
        SmallRyeThreadContext executor = smallRyeContextManager.newThreadContextBuilder()
                .propagated(Label.CONTEXT_NAME)
                .cleared(ThreadContext.ALL_REMAINING)
                .build();

        try {
            // Set non-default values
            Label.set("copy-test-label-A");

            CompletableFuture<String> noContextCF = new CompletableFuture<>();
            CompletionStage<String> noContextCFStage1 = noContextCF.thenApplyAsync(res -> {
                Assert.assertEquals(Label.get(), "",
                        "Context type should not be propagated to non-contextual action.");

                return res;
            });

            CompletionStage<String> contextCF = executor.copy(noContextCFStage1).thenApplyAsync(res -> {
                Assert.assertEquals(Label.get(), "copy-test-label-A",
                        "Context type should be propagated to contextual action.");

                return res;
            });

            noContextCF.complete("OK");
            contextCF.toCompletableFuture().get(MAX_WAIT_NS, TimeUnit.NANOSECONDS);
        } finally {
            // Restore original values
            Label.set(null);
        }
    }
}

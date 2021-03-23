package io.smallrye.context.test;

import static org.junit.Assert.assertSame;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.security.auth.Subject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.smallrye.context.SmallRyeManagedExecutor;

/**
 * The most basic level of security propagation could be the propagation of both a JAAS Subject,
 * and AccessControlContext - before vendor specific representations are added these are the
 * original approaches available.
 *
 * This test adds tests to verify a Subject can be successfully propagated.
 */
public class SecurityTest {

    private static Subject identity;
    private ExecutorService executorService;

    @BeforeClass
    public static void setupSubject() {
        Subject identity = new Subject();
        identity.setReadOnly();

        // We don't need content as we can verify the instance was propagated.
        SecurityTest.identity = identity;
    }

    @AfterClass
    public static void clearSubject() {
        identity = null;
    }

    @Before
    public void createExecutor() throws Exception {
        executorService = Executors.newSingleThreadExecutor();
        // Ensure we initialise the initial Thread so we don't accidentally
        // capture the AccessControlContext.
        Future<?> result = executorService.submit(() -> {
        });
        result.get();
    }

    @After
    public void shutDownExecutor() {
        executorService.shutdown();
        executorService = null;
    }

    @Test
    public void testManagedExecutor() {
        Subject.doAs(identity, (PrivilegedAction<Void>) () -> {
            _testManagedExecutor();
            return null;
        });
    }

    private void _testManagedExecutor() {
        assertCorrectSubject();

        ManagedExecutor executor = SmallRyeManagedExecutor.builder()
                .withExecutorService(executorService)
                .propagated(ThreadContext.SECURITY)
                .build();

        CompletableFuture<Void> future = executor.runAsync(this::assertCorrectSubject);
        future.join();

        executor.shutdown();
    }

    private void assertCorrectSubject() {
        AccessControlContext accessControllContext = AccessController.getContext();
        Subject currentSubject = Subject.getSubject(accessControllContext);

        assertSame("Same Subject", identity, currentSubject);
    }

    @Test
    public void testThreadContext() throws Exception {
        Subject.doAs(identity, (PrivilegedExceptionAction<Void>) () -> {
            _testThreadContext();
            return null;
        });
    }

    private void _testThreadContext() throws InterruptedException, ExecutionException {
        assertCorrectSubject();

        ThreadContext threadContext = ThreadContext.builder()
                .propagated(ThreadContext.SECURITY)
                .build();

        Runnable runnable = threadContext.contextualRunnable(this::assertCorrectSubject);

        Future<?> future = executorService.submit(runnable);
        future.get();
    }

    // TODO Opposite Tests, i.e. Threads with an AccessControlContext and Subject should be
    // cleared for execution.

}

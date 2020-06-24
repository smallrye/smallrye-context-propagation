package io.smallrye.context.storage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManager.Builder;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class StorageTest {

    private static StorageManagerProviderRegistration registration;

    @BeforeAll
    public static void beforeAll() {
        registration = StorageManagerProvider.register(new QuarkusStorageManagerProvider());
    }

    @AfterAll
    public static void afterAll() {
        registration.unregister();
        registration = null;
    }

    static class QuarkusThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            return new QuarkusThread(r);
        }

    }

    @Test
    public void test() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4, new QuarkusThreadFactory());
        Builder builder = ContextManagerProvider.instance().getContextManagerBuilder();
        builder.withThreadContextProviders(new RESTEasyThreadContextProvider());
        // this strategy doesn't work out
        //        builder.withThreadContextProviders(new QuarkusStorageThreadContextProvider());
        ContextManager contextManager = builder.build();
        ThreadContext tc = contextManager.newThreadContextBuilder().propagated(ThreadContext.ALL_REMAINING).build();

        // try on normal thread
        testContext(tc);

        // try on quarkus thread
        executor.execute(() -> testContext(tc));
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

    private void testContext(ThreadContext tc) {
        // make sure we have no context
        Assertions.assertNull(RESTEasyContext.getContext(Integer.class));
        // make sure we can capture the lack of context
        Runnable noContext = tc.contextualRunnable(() -> Assertions.assertNull(RESTEasyContext.getContext(Integer.class)));
        Runnable withContext;

        // add a new context level
        try (CloseableContext ctx = RESTEasyContext.newContextLevelProper()) {
            // put some context
            RESTEasyContext.pushContext(Integer.class, 42);
            // make sure we have context
            Assertions.assertEquals(42, RESTEasyContext.getContext(Integer.class));
            // make sure we can capture this context
            withContext = tc.contextualRunnable(() -> Assertions.assertEquals(42, RESTEasyContext.getContext(Integer.class)));
            // make sure we have no context when running this
            noContext.run();
        }
        // make sure the context is gone
        Assertions.assertNull(RESTEasyContext.getContext(Integer.class));
        // make sure we have context when running this
        withContext.run();
    }
}

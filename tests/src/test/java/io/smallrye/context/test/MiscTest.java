package io.smallrye.context.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.junit.Assert;
import org.junit.Test;

public class MiscTest {

    @Test
    public void testCFWrapping() {
        ContextManager contextManager = ContextManagerProvider.instance().getContextManager();
        ThreadContext threadContext = contextManager.newThreadContextBuilder().build();

        CompletionStage<String> cs = CompletableFuture.completedFuture("foo");
        CompletionStage<String> wrapped = threadContext.withContextCapture(cs);
        Assert.assertTrue(wrapped instanceof CompletableFuture);
    }
}

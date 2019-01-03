package io.smallrye.concurrency.test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyProvider;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jboss.weld.environment.se.Weld;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.smallrye.concurrency.SmallRyeConcurrencyProvider;
import io.smallrye.concurrency.impl.ThreadContextImpl;

public class ManualPropagationMultipleRequestTest {
	
	
    public class Request {

        public Map<String, Object> contextMap;
        public BoundRequestContext cdiContext;

        public Request(BoundRequestContext cdiContext, Map<String, Object> contextMap) {
            this.cdiContext = cdiContext;
            this.contextMap = contextMap;
        }

    }

    private static ThreadContextImpl threadContext;
    private static Weld weld;

	@BeforeClass
	public static void init() {
		SmallRyeConcurrencyProvider.getManager();
		threadContext = (ThreadContextImpl) ConcurrencyProvider.instance().getConcurrencyManager().newThreadContextBuilder().build();
		
        weld = new Weld();
        weld.initialize();
	}

	public Request newRequest(String reqId) {
		// seed
		MyContext.init();
		
		MyContext.get().set(reqId);
		
        BoundRequestContext cdiContext = CDI.current().select(BoundRequestContext.class).get();
        Map<String,Object> contextMap = new HashMap<String,Object>();
        cdiContext.associate(contextMap);
        cdiContext.activate();
        
        return new Request(cdiContext, contextMap);
	}
	
	public void endOfRequest(Request request) {
		MyContext.clear();

        request.cdiContext.invalidate();
        request.cdiContext.deactivate();
        request.cdiContext.dissociate(request.contextMap);
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
		Request req1 = newRequest("req 1");
		Future<?> task1 = executor.submit(threadContext.contextualRunnable(() -> {
			checkContextCaptured("req 1");
			endOfRequest(req1);
		}));
		
		Request req2 = newRequest("req 2");
		Future<?> task2 = executor.submit(threadContext.contextualRunnable(() -> {
			checkContextCaptured("req 2");
			endOfRequest(req2);
		}));

		task1.get();
		task2.get();
		executor.shutdown();
	}

	@Test
	public void testCompletionStageOnSingleWorkerThread() throws Throwable {
	    ManagedExecutor executor = ConcurrencyProvider.instance().getConcurrencyManager().newManagedExecutorBuilder().maxAsync(1).build();
		testCompletionStage(executor);
	}

	@Test
	public void testCompletionStageOnTwoWorkerThread() throws Throwable {
        ManagedExecutor executor = ConcurrencyProvider.instance().getConcurrencyManager().newManagedExecutorBuilder().maxAsync(2).build();
		testCompletionStage(executor);
	}

	private void testCompletionStage(ManagedExecutor executor) throws Throwable {
		CountDownLatch latch = new CountDownLatch(2);

		Throwable[] ret = new Throwable[2];

		Request req1 = newRequest("req 1");
		CompletableFuture<Void> cf1 = executor.newIncompleteFuture();
		cf1.handleAsync((v, t) -> {
			try {
				ret[0] = t;
				checkContextCaptured("req 1");
				endOfRequest(req1);
			}catch(Throwable t2) {
				ret[0] = t2;
			}
			latch.countDown();
			return null;
		});
		
		Request req2 = newRequest("req 2");
		CompletableFuture<Void> cf2 = executor.newIncompleteFuture();
		cf2.handleAsync((v, t) -> {
			try {
				ret[1] = t;
				checkContextCaptured("req 2");
				endOfRequest(req2);
			}catch(Throwable t2) {
				ret[1] = t2;
			}
			latch.countDown();
			return null;
		});

		cf1.complete(null);
		cf2.complete(null);
		latch.await();
		if(ret[0] != null)
			throw ret[0];
		if(ret[1] != null)
			throw ret[1];
		executor.shutdown();
	}

	private void checkContextCaptured(String reqId) {
		Assert.assertEquals(reqId, MyContext.get().getReqId());
	}
}

package io.smallrye.concurrency.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.smallrye.concurrency.SmallRyeConcurrencyProvider;
import rx.Emitter.BackpressureMode;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class RxJava1MultipleRequestTest {

	@BeforeClass
	public static void init() {
		// initialise
		SmallRyeConcurrencyProvider.getManager();
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
	public void testObservableOnSingleWorkerThread() throws Throwable {
		Executor myExecutor = Executors.newSingleThreadExecutor();
		testObservable(Schedulers.from(myExecutor));
	}
	
	@Test
	public void testObservableOnNewWorkerThreads() throws Throwable {
		testObservable(Schedulers.newThread());
	}
	
	private void testObservable(Scheduler scheduler) throws Throwable {
		CountDownLatch latch = new CountDownLatch(4);

		Throwable[] ret = new Throwable[1];
		
		newRequest("req 1");
		Observable.create(emitter -> {
			checkContextCaptured("req 1");
			new Thread(() -> {
				emitter.onNext("a");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				emitter.onNext("b");
				emitter.onCompleted();
			}).start();
		}, BackpressureMode.BUFFER)
		.subscribeOn(scheduler)
		.observeOn(scheduler)
		.doOnCompleted(() -> {
			checkContextCaptured("req 1");
			endOfRequest();
		})
		.subscribe(success -> {
			checkContextCaptured("req 1");
			latch.countDown();
		}, error -> {
			ret[0] = error;
			latch.countDown();
			latch.countDown();
		});

		newRequest("req 2");
		Observable.create(emitter -> {
			checkContextCaptured("req 2");
			new Thread(() -> {
				emitter.onNext("a");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				emitter.onNext("b");
				emitter.onCompleted();
			}).start();
		}, BackpressureMode.BUFFER)
		.subscribeOn(scheduler)
		.observeOn(scheduler)
		.doOnCompleted(() -> {
			checkContextCaptured("req 2");
			endOfRequest();
		})
		.subscribe(success -> {
			checkContextCaptured("req 2");
			latch.countDown();
		}, error -> {
			ret[0] = error;
			latch.countDown();
			latch.countDown();
		});

		latch.await();
		if (ret[0] != null)
			throw ret[0];
	}

	private void checkContextCaptured(String reqId) {
		Assert.assertEquals(reqId, MyContext.get().getReqId());
	}
}

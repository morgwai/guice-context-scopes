// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.*;
import pl.morgwai.base.guice.scopes.ContextTrackingExecutor.DetailedRejectedExecutionException;

import static org.junit.Assert.*;



public class ContextTrackingExecutorTest {



	public static final long TIMEOUT_MILLIS = 500L;

	final ContextTracker<TestContext1> tracker1 = new ContextTracker<>();
	final ContextTracker<TestContext2> tracker2 = new ContextTracker<>();
	final ContextTracker<TestContext3> tracker3 = new ContextTracker<>();
	final List<ContextTracker<?>> allTrackers = List.of(tracker1, tracker2, tracker3);

	final TestContext1 ctx1 = new TestContext1(tracker1);
	final TestContext2 ctx2 = new TestContext2(tracker2);
	final TestContext3 ctx3 = new TestContext3(tracker3);
	final List<TrackableContext<?>> allCtxs = List.of(ctx1, ctx2, ctx3);

	static final int POOL_SIZE = 2;
	static final int QUEUE_SIZE = 1;
	final ContextTrackingExecutor executor = new ContextTrackingExecutor(
			"testExecutor", POOL_SIZE, allTrackers, new LinkedBlockingQueue<>(QUEUE_SIZE));
	static final String TASK_NAME = "testTask";

	AssertionError asyncAssertionError;



	@Test
	public void testGetActiveContexts() {
		ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(
				() -> {
					final var activeCtxs = ContextTrackingExecutor.getActiveContexts(allTrackers);
					assertEquals("there should be 2 active ctxs",  2, activeCtxs.size());
					assertTrue("ctx1 should be active", activeCtxs.contains(ctx1));
					assertTrue("ctx3 should be active", activeCtxs.contains(ctx3));
				}
			)
		);
	}



	@Test
	public void testExecuteWithinAll() {
		ContextTrackingExecutor.executeWithinAll(  // method under test
			allCtxs,
			() -> {
				assertSame("ctx1 should be active", ctx1, tracker1.getCurrentContext());
				assertSame("ctx2 should be active", ctx2, tracker2.getCurrentContext());
				assertSame("ctx3 should be active", ctx3, tracker3.getCurrentContext());
			}
		);
	}



	@Test
	public void testExecuteCallableWithinAll() throws Exception {
		final var result = "result";
		final var obtained = ContextTrackingExecutor.executeWithinAll(  // method under test
			allCtxs,
			() -> {
				assertSame("ctx1 should be active", ctx1, tracker1.getCurrentContext());
				assertSame("ctx2 should be active", ctx2, tracker2.getCurrentContext());
				assertSame("ctx3 should be active", ctx3, tracker3.getCurrentContext());
				return result;
			}
		);
		assertSame("result should match", result, obtained);
	}



	@Test
	public void testExecutingRunnablePropagatesRuntimeException() {
		final var thrown = new RuntimeException("thrown");
		final Runnable throwingTask = () -> { throw  thrown; };
		try {
			ContextTrackingExecutor.executeWithinAll(allCtxs, throwingTask);
			fail("RuntimeException thrown by the task should be propagated");
		} catch (RuntimeException caught) {
			assertSame("caught exception should be the same as thrown", thrown, caught);
		}
	}



	@Test
	public void testExecute() throws Exception {
		final var latch = new CountDownLatch(1);
		ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(
				() -> executor.execute(() -> {  // method under test
					try {
						assertSame("ctx1 should be active", ctx1, tracker1.getCurrentContext());
						assertSame("ctx3 should be active", ctx3, tracker3.getCurrentContext());
						assertNull("ctx2 should not be active", tracker2.getCurrentContext());
					} catch (AssertionError e) {
						asyncAssertionError = e;
					} finally {
						latch.countDown();
					}
				})
			)
		);
		if ( !latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) throw new TimeoutException();
		if (asyncAssertionError != null) throw asyncAssertionError;
	}



	@Test
	public void testExecuteCallable() throws Exception {
		final var result = "result";
		final var callFuture = ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(
				() -> executor.execute(() -> {  // method under test
					try {
						assertSame("ctx1 should be active", ctx1, tracker1.getCurrentContext());
						assertSame("ctx3 should be active", ctx3, tracker3.getCurrentContext());
						assertNull("ctx2 should not be active", tracker2.getCurrentContext());
						return result;
					} catch (AssertionError e) {
						asyncAssertionError = e;
						throw e;
					}
				})
			)
		);
		final var obtained = callFuture.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		if (asyncAssertionError != null) throw asyncAssertionError;
		assertSame("obtained object should be the same as returned", result, obtained);
	}



	@Test
	public void testExecuteThrowingCallableAndCallGet() throws Exception {
		final var thrown = new Exception("thrown");

		final var callFuture = ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(
				() -> executor.execute(() -> {  // method under test
					try {
						assertSame("ctx1 should be active", ctx1, tracker1.getCurrentContext());
						assertSame("ctx3 should be active", ctx3, tracker3.getCurrentContext());
						assertNull("ctx2 should not be active", tracker2.getCurrentContext());
						throw thrown;
					} catch (AssertionError e) {
						asyncAssertionError = e;
						throw e;
					}
				})
			)
		);

		try {
			callFuture.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
			fail("ExecutionException should be thrown if the task throws any Exception");
		} catch (ExecutionException e) {
			if (asyncAssertionError != null) throw asyncAssertionError;
			assertSame("cause of the ExecutionException should be the same as thrown by the task",
					thrown, e.getCause());
		}
	}



	@Test
	public void testExecuteThrowingCallableAndCallWhenComplete() throws Exception {
		final var thrown = new Exception("thrown");

		final var callFuture = ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(
				() -> executor.execute(() -> {  // method under test
					try {
						assertSame("ctx1 should be active", ctx1, tracker1.getCurrentContext());
						assertSame("ctx3 should be active", ctx3, tracker3.getCurrentContext());
						assertNull("ctx2 should not be active", tracker2.getCurrentContext());
						throw thrown;
					} catch (AssertionError e) {
						asyncAssertionError = e;
						throw e;
					}
				})
			)
		);
		final var completionLatch = new CountDownLatch(1);
		callFuture.whenComplete(
			(result, caught) -> {
				try {
					assertSame("caught exception should be the same as thrown by the Callable task",
							thrown, caught);
					assertNull("result should be null if the task throws an Exception", result);
				} catch (AssertionError e) {
					asyncAssertionError = e;
				} finally {
					completionLatch.countDown();
				}
			}
		);

		assertTrue("the Callable task should complete",
				completionLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
		if (asyncAssertionError != null) throw asyncAssertionError;
	}



	/**
	 * Makes all executor's thread busy and fills its queue.
	 * @param barrier the barrier on which executor threads will await.
	 */
	void fullyLoadExecutor(CyclicBarrier barrier) {
		ctx1.executeWithinSelf(
			() -> {
				for (int i = 0; i < POOL_SIZE; i++) {  // make all threads busy
					executor.execute(
						() -> {
							try {
								barrier.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
							} catch (Exception ignored) {}
						}
					);
				}
				for (int i = 0; i < QUEUE_SIZE; i++) executor.execute(() -> {});  // fill the queue
			}
		);
	}



	@Test
	public void testExecutionRejection() throws Exception {
		final var barrier = new CyclicBarrier(POOL_SIZE + 1);
		final var overloadingTask = new Runnable() {
			@Override public void run() {}
			@Override public String toString() { return TASK_NAME; }
		};
		fullyLoadExecutor(barrier);

		try {
			executor.execute(overloadingTask);  // method under test
			fail("overloaded executor should throw a DetailedRejectedExecutionException");
		} catch (DetailedRejectedExecutionException rejection) {
			assertSame("the rejection should contain the executor which threw this rejection",
					executor, rejection.getExecutor());
			assertSame("unwrapped rejected task should be the one that overloaded the executor",
					overloadingTask, rejection.getTask());
		} finally {
			barrier.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		}
	}



	@Test
	public void testCallableExecutionRejection() throws Exception {
		final var barrier = new CyclicBarrier(POOL_SIZE + 1);
		final var overloadingTask = new Callable<>() {
			@Override public String call() { return ""; }
			@Override public String toString() { return TASK_NAME; }
		};
		fullyLoadExecutor(barrier);

		try {
			executor.execute(overloadingTask);  // method under test
			fail("overloaded executor should throw a DetailedRejectedExecutionException");
		} catch (DetailedRejectedExecutionException rejection) {
			assertSame("the rejection should contain the executor which threw this rejection",
					executor, rejection.getExecutor());
			assertSame("unwrapped rejected task should be the one that overloaded the executor",
					overloadingTask, rejection.getTask());
		} finally {
			barrier.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		}
	}



	@Test
	public void testEnforceTerminationCallsShutdownNowOnUncheckedException()
			throws InterruptedException {
		final var backingExecutor = new ThreadPoolExecutor(
				1, 1, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<>()) {

			boolean shutdownNowCalled;

			@Override public boolean awaitTermination(long timeout, TimeUnit unit) {
				throw new InternalError("unchecked");
			}

			@Override public List<Runnable> shutdownNow() {
				shutdownNowCalled = true;
				return super.shutdownNow();
			}
		};
		final var testSubject = new ContextTrackingExecutor(
				"testSubject", 1, allTrackers, backingExecutor);

		try {
			testSubject.enforceTermination(10L, TimeUnit.MILLISECONDS);
			fail("InternalError expected");
		} catch (InternalError expected) {}
		assertTrue("shutdownNow() should be called", backingExecutor.shutdownNowCalled);
	}



	@After
	public void shutdown() throws InterruptedException {
		executor.shutdown();
		executor.enforceTermination(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}



	@BeforeClass
	public static void setupLogging() {
		for (final var handler: Logger.getLogger("").getHandlers()) handler.setLevel(Level.SEVERE);
	}



	static class TestContext1 extends TrackableContext<TestContext1> {
		TestContext1(ContextTracker<TestContext1> tracker) { super(tracker); }
	}

	static class TestContext2 extends TrackableContext<TestContext2> {
		TestContext2(ContextTracker<TestContext2> tracker) { super(tracker); }
	}

	static class TestContext3 extends TrackableContext<TestContext3> {
		TestContext3(ContextTracker<TestContext3> tracker) { super(tracker); }
	}
}

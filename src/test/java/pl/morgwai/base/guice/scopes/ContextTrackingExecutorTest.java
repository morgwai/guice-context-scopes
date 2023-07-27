// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.*;

import org.junit.*;

import static org.junit.Assert.*;



public class ContextTrackingExecutorTest {



	public static final long TIMEOUT_MILLIS = 50L;

	final ContextTracker<TestContext1> tracker1 = new ContextTracker<>();
	final ContextTracker<TestContext2> tracker2 = new ContextTracker<>();
	final ContextTracker<TestContext3> tracker3 = new ContextTracker<>();
	final List<ContextTracker<?>> allTrackers = List.of(tracker1, tracker2, tracker3);

	final TestContext1 ctx1 = new TestContext1(tracker1);
	final TestContext2 ctx2 = new TestContext2(tracker2);
	final TestContext3 ctx3 = new TestContext3(tracker3);
	final List<TrackableContext<?>> allCtxs = List.of(ctx1, ctx2, ctx3);

	Runnable rejectedTask;
	Executor rejectingExecutor;
	protected final RejectedExecutionHandler rejectionHandler = (task, executor) -> {
		rejectedTask = task;
		rejectingExecutor = executor;
		throw new RejectedExecutionException("rejected " + task);
	};

	static final int POOL_SIZE = 2;
	static final int QUEUE_SIZE = 1;

	final ThreadPoolExecutor backingExecutor = new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, 0L,
			TimeUnit.DAYS, new LinkedBlockingQueue<>(QUEUE_SIZE), rejectionHandler);
	final ContextTrackingExecutor testSubject =
			new ContextTrackingExecutor(allTrackers, backingExecutor);

	AssertionError asyncAssertionError;



	@After
	public void tryTerminate() {
		testSubject.shutdown();
		try {
			testSubject.awaitTermination(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ignored) {
		} finally {
			if ( !testSubject.isTerminated()) testSubject.shutdownNow();
		}
	}



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
	public void testExecutingRunnableWithinAllPropagatesRuntimeException() {
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
	public void testExecute() throws InterruptedException {
		final var taskFinished = new CountDownLatch(1);
		ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(
				() -> testSubject.execute(() -> {  // method under test
					try {
						assertSame("ctx1 should be active", ctx1, tracker1.getCurrentContext());
						assertSame("ctx3 should be active", ctx3, tracker3.getCurrentContext());
						assertNull("ctx2 should not be active", tracker2.getCurrentContext());
					} catch (AssertionError e) {
						asyncAssertionError = e;
					} finally {
						taskFinished.countDown();
					}
				})
			)
		);
		assertTrue("the submitted task should finish",
				taskFinished.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
		if (asyncAssertionError != null) throw asyncAssertionError;
	}



	/** Blocks all threads of executor on {@code taskBlockingLatch}. */
	void blockAllExecutorThreads(CountDownLatch taskBlockingLatch, CountDownLatch signalStart) {
		ctx1.executeWithinSelf(() -> {
			for (int i = 0; i < POOL_SIZE; i++) {  // make all threads busy
				testSubject.execute(() -> {
					if (signalStart != null) signalStart.countDown();
					try {
						taskBlockingLatch.await();
					} catch (InterruptedException ignored) {}
				});
			}
		});
	}



	@Test
	public void testExecutionRejection() {
		final Runnable overloadingTask = () -> {};
		final var taskBlockingLatch = new CountDownLatch(1);
		try {
			blockAllExecutorThreads(taskBlockingLatch, null);
			ctx1.executeWithinSelf(() -> {  // fill the queue
				for (int i = 0; i < QUEUE_SIZE; i++) testSubject.execute(() -> {});
			});

			testSubject.execute(overloadingTask);  // method under test
			fail("overloaded executor should throw a RejectedExecutionException");
		} catch (RejectedExecutionException expected) {
		} finally {
			taskBlockingLatch.countDown();
		}
		assertSame("rejectingExecutor should be backingExecutor",
				backingExecutor, rejectingExecutor);
		assertSame("rejectedTask should be overloadingTask", overloadingTask, rejectedTask);
	}



	@Test
	public void testShutdownNowUnwrapsTasks() throws InterruptedException {
		final Runnable queuedTask = () -> {};
		final var allBlockingTasksStarted = new CountDownLatch(POOL_SIZE);
		final var taskBlockingLatch = new CountDownLatch(1);
		try {
			blockAllExecutorThreads(taskBlockingLatch, allBlockingTasksStarted);
			testSubject.execute(queuedTask);
			assertTrue("all blocking tasks should start",
					allBlockingTasksStarted.await(50L, TimeUnit.MILLISECONDS));

			final var unexecutedTasks = testSubject.shutdownNow();
			assertEquals("there should be 1 unexecuted task after shutdownNow()",
					1, unexecutedTasks.size());
			assertSame("unexecuted task should be queuedTask", queuedTask, unexecutedTasks.get(0));
		} finally {
			taskBlockingLatch.countDown();
		}
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

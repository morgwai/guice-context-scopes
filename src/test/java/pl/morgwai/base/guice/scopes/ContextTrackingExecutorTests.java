// Copyright 2024 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.*;
import org.junit.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertTrue;
import static pl.morgwai.base.guice.scopes.TestContexts.*;



public class ContextTrackingExecutorTests {



	final ContextBinder ctxBinder = new ContextBinder(List.of(tracker));



	protected ContextTrackingExecutor testSubject;
	protected ExecutorService executorToShutdown;

	@Before
	public void setup() {
		executorToShutdown = Executors.newSingleThreadExecutor();
		testSubject = ContextTrackingExecutor.of(executorToShutdown, ctxBinder);
	}



	Throwable asyncError;

	@Test
	public void testExecute() throws Throwable {
		final var taskCompleted = new CountDownLatch(1);
		final var ctx = new TestContext(tracker);
		final Runnable testTask = () -> {
			if (tracker.getCurrentContext() != ctx) {
				asyncError = new AssertionError("testTask should be bound to ctx");
			}
			taskCompleted.countDown();
		};
		ctx.executeWithinSelf(() -> testSubject.execute(testTask));
		assertTrue("testTask should complete",
				taskCompleted.await(50L, MILLISECONDS));
		if (asyncError != null) throw asyncError;
	}



	@After
	public void shutdown() throws InterruptedException {
		executorToShutdown.shutdown();
		try {
			assertTrue("testSubject Executor should terminate cleanly",
					executorToShutdown.awaitTermination(50L, MILLISECONDS));
		} finally {
			if ( !executorToShutdown.isTerminated()) executorToShutdown.shutdownNow();
		}
	}
}

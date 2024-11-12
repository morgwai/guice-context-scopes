// Copyright 2024 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.*;
import org.junit.After;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertTrue;
import static pl.morgwai.base.guice.scopes.TestContexts.*;



public class ContextTrackingExecutorTests {



	final ContextBinder ctxBinder = new ContextBinder(List.of(tracker));
	final ExecutorService wrappedExecutor = Executors.newSingleThreadExecutor();
	final ContextTrackingExecutor testSubject =
			ContextTrackingExecutor.of(wrappedExecutor, ctxBinder);

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
		wrappedExecutor.shutdown();
		try {
			assertTrue("testSubject Executor should terminate cleanly",
				wrappedExecutor.awaitTermination(50L, MILLISECONDS));
		} finally {
			if ( !wrappedExecutor.isTerminated()) wrappedExecutor.shutdownNow();
		}
	}
}

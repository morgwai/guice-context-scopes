// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import org.junit.Test;

import static org.junit.Assert.*;



public class ContextTrackerTest {



	final ContextTracker<TestContext> tracker = new ContextTracker<>();
	final TestContext ctx = new TestContext(tracker);



	@Test
	public void testTrackingRunnable() {
		assertNull("current context should be unset initially", tracker.getCurrentContext());
		ctx.executeWithinSelf(
			() -> assertSame("executing context should be set as the current",
					ctx, tracker.getCurrentContext())
		);
		assertNull("current context should be cleared at the end", tracker.getCurrentContext());
	}



	@Test
	public void testTrackingCallable() throws Exception {
		final var result = "result";
		assertNull("current context should be unset initially", tracker.getCurrentContext());
		final var obtained = ctx.executeWithinSelf(
			() -> {
				assertSame("executing context should be set as the current",
						ctx, tracker.getCurrentContext());
				return result;
			}
		);
		assertNull("current context should be cleared at the end", tracker.getCurrentContext());
		assertSame("obtained object should be the same as returned", result, obtained);
	}



	@Test
	public void testExecutingRunnablePropagatesRuntimeException() {
		final var thrown = new RuntimeException("thrown");
		final Runnable throwingTask = () -> { throw  thrown; };
		try {
			ctx.executeWithinSelf(throwingTask);
			fail("RuntimeException thrown by the task should be propagated");
		} catch (RuntimeException caught) {
			assertSame("caught exception should be the same as thrown", thrown, caught);
		}
	}



	@Test
	public void testTrackingAcrossThreads() throws Exception {
		final AssertionError[] errorHolder = {null};
		final Runnable ctxVerifyingTask = () -> assertSame(
				"executing context should be set as the current", ctx, tracker.getCurrentContext());
		ctx.executeWithinSelf(() -> {
			final var currentContext = tracker.getCurrentContext();
			final var thread = new Thread(() -> {
				try {
					assertNull("current context should be unset initially in a new thread",
							tracker.getCurrentContext());
					currentContext.executeWithinSelf(ctxVerifyingTask);
					assertNull("current context should be cleared at the end",
							tracker.getCurrentContext());
				} catch (AssertionError e) {
					errorHolder[0] = e;
				}
			});
			thread.start();
			thread.join();
			if (errorHolder[0] != null) throw errorHolder[0];
			return "";
		});
	}



	static class TestContext extends TrackableContext<TestContext> {
		TestContext(ContextTracker<TestContext> tracker) { super(tracker); }
	}
}

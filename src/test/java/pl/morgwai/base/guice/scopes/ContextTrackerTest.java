// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import org.junit.Test;

import static org.junit.Assert.*;



public class ContextTrackerTest {



	final ContextTracker<TestContext> tracker = new ContextTracker<>();
	final TestContext ctx = new TestContext(tracker);



	@Test
	public void testTrackingRunnable() {
		assertNull("context should be unset initially", tracker.getCurrentContext());
		ctx.executeWithinSelf(
			() -> assertSame("context should be set", ctx, tracker.getCurrentContext()));
		assertNull("context should be cleared", tracker.getCurrentContext());
	}



	@Test
	public void testTrackingCallable() throws Exception {
		final var result = new Object();
		assertNull("context should be unset initially", tracker.getCurrentContext());
		final var obtained = ctx.executeWithinSelf(
			() -> {
				assertSame("context should be set", ctx, tracker.getCurrentContext());
				return result;
			}
		);
		assertNull("context should be cleared", tracker.getCurrentContext());
		assertSame("obtained object should be the same as returned", result, obtained);
	}



	@Test
	public void testExecutingRunnablePropagatesRuntimeException() {
		final var thrown = new RuntimeException();
		final Runnable task = () -> { throw  thrown; };
		try {
			ctx.executeWithinSelf(task);
			fail("RuntimeException expected");
		} catch (RuntimeException caught) {
			assertSame("caught exception should be the same as thrown", thrown, caught);
		}
	}



	@Test
	public void testTrackingAcrossThreads() throws Exception {
		final AssertionError[] errorHolder = {null};
		ctx.executeWithinSelf(() -> {
			final var currentContext = tracker.getCurrentContext();
			final var thread = new Thread(() -> {
				try {
					assertNull("context should be unset initially", tracker.getCurrentContext());
					currentContext.executeWithinSelf(
						() -> assertSame("context should be set",
							ctx, tracker.getCurrentContext())
					);
					assertNull("context should be cleared", tracker.getCurrentContext());
				} catch (AssertionError e) {
					errorHolder[0] = e;
				}
			});
			thread.start();
			thread.join();
			return "";
		});
		if (errorHolder[0] != null) throw errorHolder[0];
	}



	static class TestContext extends TrackableContext<TestContext> {
		TestContext(ContextTracker<TestContext> tracker) { super(tracker); }
	}
}

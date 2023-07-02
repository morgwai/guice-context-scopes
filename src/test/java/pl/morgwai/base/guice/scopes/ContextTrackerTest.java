// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import org.junit.Test;

import static org.junit.Assert.*;



public class ContextTrackerTest {



	final ContextTracker<TestContext> tracker = new ContextTracker<>();



	@Test
	public void testTrackingRunnable() {
		final var ctx = new TestContext(tracker);
		assertNull("context should be unset initially", tracker.getCurrentContext());
		ctx.executeWithinSelf(
				() -> assertSame("context should be set", ctx, tracker.getCurrentContext()));
		assertNull("context should be cleared", tracker.getCurrentContext());
	}



	@Test
	public void testTrackingCallable() throws Exception {
		final var result = new Object();
		final var ctx = new TestContext(tracker);
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
			new TestContext(tracker).executeWithinSelf(task);
			fail("RuntimeException expected");
		} catch (RuntimeException caught) {
			assertSame("caught exception should be the same as thrown", thrown, caught);
		}
	}



	@Test
	public void testTrackingAcrossThreads() throws Exception {
		final var originalCtx = new TestContext(tracker);
		final AssertionError[] errorHolder = {null};
		originalCtx.executeWithinSelf(() -> {
			final var ctx = tracker.getCurrentContext();
			final var thread = new Thread(() -> {
				try {
					assertNull("context should be unset initially", tracker.getCurrentContext());
					ctx.executeWithinSelf(
						() -> assertSame("context should be set",
								originalCtx, tracker.getCurrentContext())
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

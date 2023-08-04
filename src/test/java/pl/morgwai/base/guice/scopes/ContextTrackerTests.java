// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;



public class ContextTrackerTests {



	final ContextTracker<TestContext1> tracker1 = new ContextTracker<>();
	final ContextTracker<TestContext2> tracker2 = new ContextTracker<>();
	final ContextTracker<TestContext3> tracker3 = new ContextTracker<>();
	final List<ContextTracker<?>> allTrackers = List.of(tracker1, tracker2, tracker3);

	final TestContext1 ctx1 = new TestContext1(tracker1);
	final TestContext3 ctx3 = new TestContext3(tracker3);



	@Test
	public void testTrackingRunnable() {
		assertNull("current context should be unset initially", tracker1.getCurrentContext());
		ctx1.executeWithinSelf(
			() -> assertSame("executing context should be set as the current",
				ctx1, tracker1.getCurrentContext())
		);
		assertNull("current context should be cleared at the end", tracker1.getCurrentContext());
	}



	@Test
	public void testTrackingCallable() throws Exception {
		final var result = "result";
		assertNull("current context should be unset initially", tracker1.getCurrentContext());
		final var obtained = ctx1.executeWithinSelf(
			() -> {
				assertSame("executing context should be set as the current",
					ctx1, tracker1.getCurrentContext());
				return result;
			}
		);
		assertNull("current context should be cleared at the end", tracker1.getCurrentContext());
		assertSame("obtained object should be the same as returned", result, obtained);
	}



	@Test
	public void testExecutingRunnablePropagatesRuntimeException() {
		final var thrown = new RuntimeException("thrown");
		final Runnable throwingTask = () -> { throw  thrown; };
		try {
			ctx1.executeWithinSelf(throwingTask);
			fail("RuntimeException thrown by the task should be propagated");
		} catch (RuntimeException caught) {
			assertSame("caught exception should be the same as thrown", thrown, caught);
		}
	}



	@Test
	public void testTrackingAcrossThreads() throws Exception {
		final AssertionError[] errorHolder = {null};
		final Runnable ctxVerifyingTask =
				() -> assertSame("executing context should be set as the current",
						ctx1, tracker1.getCurrentContext());
		ctx1.executeWithinSelf(() -> {
			final var currentContext = tracker1.getCurrentContext();
			final var thread = new Thread(() -> {
				try {
					assertNull("current context should be unset initially in a new thread",
							tracker1.getCurrentContext());
					currentContext.executeWithinSelf(ctxVerifyingTask);
					assertNull("current context should be cleared at the end",
							tracker1.getCurrentContext());
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



	@Test
	public void testGetActiveContexts() {
		ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(
				() -> {
					final var activeCtxs = ContextTracker.getActiveContexts(allTrackers);
					assertEquals("there should be 2 active ctxs",  2, activeCtxs.size());
					assertTrue("ctx1 should be active", activeCtxs.contains(ctx1));
					assertTrue("ctx3 should be active", activeCtxs.contains(ctx3));
				}
			)
		);
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

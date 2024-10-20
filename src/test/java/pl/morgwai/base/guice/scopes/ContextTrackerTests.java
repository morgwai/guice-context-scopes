// Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;
import static pl.morgwai.base.guice.scopes.ContextTracker.getActiveContexts;
import static pl.morgwai.base.guice.scopes.TestContexts.*;



public class ContextTrackerTests {



	final TestContext ctx1 = new TestContext(tracker);
	final ThirdTestContext ctx3 = new ThirdTestContext(thirdTracker);



	@Test
	public void testTrackingRunnable() {
		assertNull("current context should be unset initially", tracker.getCurrentContext());
		ctx1.executeWithinSelf(
			() -> assertSame("executing context should be set as the current",
					ctx1, tracker.getCurrentContext())
		);
		assertNull("current context should be cleared at the end", tracker.getCurrentContext());
	}



	@Test
	public void testTrackingCallable() throws Exception {
		final var result = "result";
		assertNull("current context should be unset initially", tracker.getCurrentContext());
		final var obtained = ctx1.executeWithinSelf(
			() -> {
				assertSame("executing context should be set as the current",
						ctx1, tracker.getCurrentContext());
				return result;
			}
		);
		assertNull("current context should be cleared at the end", tracker.getCurrentContext());
		assertSame("obtained object should be the same as returned",
				result, obtained);
	}



	@Test
	public void testTrackingAcrossThreads() throws Exception {
		final AssertionError[] errorHolder = {null};
		ctx1.executeWithinSelf(() -> {
			final var currentContext = tracker.getCurrentContext();
			final var thread = new Thread(() -> {
				try {
					assertNull("current context should be unset initially in a new thread",
							tracker.getCurrentContext());
					currentContext.executeWithinSelf(
						() -> assertSame("executing context should be set as the current",
									ctx1, tracker.getCurrentContext())
					);
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



	@Test
	public void testGetActiveContextsMultipleTrackers() {
		ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(
				() -> {
					final var activeCtxs = getActiveContexts(allTrackers);
					assertEquals("there should be 2 active ctxs",
							2, activeCtxs.size());
					assertTrue("ctx1 should be active", activeCtxs.contains(ctx1));
					assertTrue("ctx3 should be active", activeCtxs.contains(ctx3));
				}
			)
		);
	}



	@Test
	public void testGetActiveContextsSingleTracker() {
		ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(
				() -> {
					final var activeCtxs = getActiveContexts(List.of(tracker));
					assertEquals("there should be 1 active ctx",
							1, activeCtxs.size());
					assertTrue("ctx1 should be active", activeCtxs.contains(ctx1));
				}
			)
		);
	}



	@Test
	public void testGetActiveContextsSingleTrackerWithInactiveContext() {
		ctx3.executeWithinSelf(
			() -> assertTrue("there should be no active ctxs",
					getActiveContexts(List.of(tracker)).isEmpty())
		);
	}



	@Test
	public void testGetActiveContextsNoTrackers() {
		ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(
				() -> assertTrue("there should be no active ctxs",
						getActiveContexts(List.of()).isEmpty())
			)
		);
	}
}

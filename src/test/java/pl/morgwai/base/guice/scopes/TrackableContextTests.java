// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;
import static pl.morgwai.base.guice.scopes.TestContexts.*;
import static pl.morgwai.base.guice.scopes.TrackableContext.executeWithinAll;



public class TrackableContextTests {



	final TestContext ctx1 = new TestContext(tracker);
	final SecondTestContext ctx2 = new SecondTestContext(secondTracker);
	final ThirdTestContext ctx3 = new ThirdTestContext(thirdTracker);
	final List<TrackableContext<?>> allCtxs = List.of(ctx1, ctx2, ctx3);

	static final String RESULT = "result";



	@Test
	public void testExecuteWithinAllMultipleCtxs() {
		final Runnable task = () -> {
			assertSame("ctx1 should be active", ctx1, tracker.getCurrentContext());
			assertSame("ctx2 should be active", ctx2, secondTracker.getCurrentContext());
			assertSame("ctx3 should be active", ctx3, thirdTracker.getCurrentContext());
		};
		executeWithinAll(allCtxs, task);
		assertSame("result should match",
				RESULT, executeWithinAll(allCtxs, () -> { task.run(); return RESULT; }));
	}



	@Test
	public void testExecuteWithinAllSingleCtx() {
		final Runnable task = () -> {
			assertSame("ctx1 should be active", ctx1, tracker.getCurrentContext());
			assertNull("ctx2 should not be active", secondTracker.getCurrentContext());
			assertNull("ctx3 should not be active", thirdTracker.getCurrentContext());
		};
		executeWithinAll(List.of(ctx1), task);
		assertSame("result should match",
				RESULT, executeWithinAll(List.of(ctx1), () -> { task.run(); return RESULT; }));
	}



	@Test
	public void testExecuteWithinAllNoCtxs() {
		final var noCtxTestTask = new Runnable() {
			@Override public void run() {
				assertNull("ctx1 should not be active", tracker.getCurrentContext());
				assertNull("ctx2 should not be active", secondTracker.getCurrentContext());
				assertNull("ctx3 should not be active", thirdTracker.getCurrentContext());
			}
			@Override public String toString() {
				return "noCtxTestTask";
			}
		};
		executeWithinAll(List.of(), noCtxTestTask);
		assertSame("result should match",
				RESULT, executeWithinAll(List.of(), () -> { noCtxTestTask.run(); return RESULT; }));
	}



	@Test
	public void testExecutingRunnableWithinAllPropagatesRuntimeException() {
		final var thrown = new RuntimeException("thrown");
		final Runnable throwingTask = () -> { throw  thrown; };
		try {
			executeWithinAll(allCtxs, throwingTask);
			fail("RuntimeException thrown by the task should be propagated");
		} catch (RuntimeException caught) {
			assertSame("caught exception should be the same as thrown",
					thrown, caught);
		}
	}



	@Test
	public void testExecutingRunnablePropagatesRuntimeException() {
		final var thrown = new RuntimeException("thrown");
		final Runnable throwingTask = () -> { throw  thrown; };
		try {
			ctx1.executeWithinSelf(throwingTask);
			fail("RuntimeException thrown by the task should be propagated");
		} catch (RuntimeException caught) {
			assertSame("caught exception should be the same as thrown",
					thrown, caught);
		}
	}



	@Test
	public void testSetTracker() {
		final var ctx = new TestContext(null);
		ctx.setTracker(tracker);
		try {
			ctx.setTracker(tracker);
			fail("resetting tracker should throw an IllegalStateException");
		} catch (IllegalStateException expected) {}
		try {
			ctx1.setTracker(tracker);
			fail("resetting tracker should throw an IllegalStateException");
		} catch (IllegalStateException expected) {}
	}
}

// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;
import static pl.morgwai.base.guice.scopes.TrackableContext.executeWithinAll;



public class TrackableContextTests {



	final ContextTracker<TestContext1> tracker1 = new ContextTracker<>();
	final ContextTracker<TestContext2> tracker2 = new ContextTracker<>();
	final ContextTracker<TestContext3> tracker3 = new ContextTracker<>();

	final TestContext1 ctx1 = new TestContext1(tracker1);
	final TestContext2 ctx2 = new TestContext2(tracker2);
	final TestContext3 ctx3 = new TestContext3(tracker3);
	final List<TrackableContext<?>> allCtxs = List.of(ctx1, ctx2, ctx3);



	@Test
	public void testExecuteWithinAllMultipleCtxs() {
		executeWithinAll(  // method under test
			allCtxs,
			() -> {
				assertSame("ctx1 should be active", ctx1, tracker1.getCurrentContext());
				assertSame("ctx2 should be active", ctx2, tracker2.getCurrentContext());
				assertSame("ctx3 should be active", ctx3, tracker3.getCurrentContext());
			}
		);
	}



	@Test
	public void testExecuteWithinAllSingleCtx() {
		executeWithinAll(  // method under test
			List.of(ctx1),
			() -> {
				assertSame("ctx1 should be active", ctx1, tracker1.getCurrentContext());
				assertNull("ctx2 should not be active", tracker2.getCurrentContext());
				assertNull("ctx3 should not be active", tracker3.getCurrentContext());
			}
		);
	}



	@Test
	public void testExecuteWithinAllNoCtxs() {
		executeWithinAll(  // method under test
			List.of(),
			new Runnable() {
				@Override public void run() {
					assertNull("ctx1 should not be active", tracker1.getCurrentContext());
					assertNull("ctx2 should not be active", tracker2.getCurrentContext());
					assertNull("ctx3 should not be active", tracker3.getCurrentContext());
				}
				@Override public String toString() {
					return "NoCtxTestTask";
				}
			}
		);
	}



	@Test
	public void testExecuteCallableWithinAll() throws Exception {
		final var result = "result";
		final var obtained = executeWithinAll(  // method under test
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
		final var ctx = new TestContext1(null);
		ctx.setTracker(tracker1);
		try {
			ctx.setTracker(tracker1);
			fail("resetting tracker should throw an IllegalStateException");
		} catch (IllegalStateException expected) {}
		try {
			ctx1.setTracker(tracker1);
			fail("resetting tracker should throw an IllegalStateException");
		} catch (IllegalStateException expected) {}
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

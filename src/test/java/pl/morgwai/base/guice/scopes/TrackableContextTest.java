// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;

import org.junit.*;

import static org.junit.Assert.*;
import static pl.morgwai.base.guice.scopes.TrackableContext.executeWithinAll;



public class TrackableContextTest {



	final ContextTracker<TestContext1> tracker1 = new ContextTracker<>();
	final ContextTracker<TestContext2> tracker2 = new ContextTracker<>();
	final ContextTracker<TestContext3> tracker3 = new ContextTracker<>();

	final TestContext1 ctx1 = new TestContext1(tracker1);
	final TestContext2 ctx2 = new TestContext2(tracker2);
	final TestContext3 ctx3 = new TestContext3(tracker3);
	final List<TrackableContext<?>> allCtxs = List.of(ctx1, ctx2, ctx3);



	@Test
	public void testExecuteWithinAll() {
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
			assertSame("caught exception should be the same as thrown", thrown, caught);
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

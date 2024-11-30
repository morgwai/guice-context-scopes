// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

import pl.morgwai.base.function.ThrowingComputation;

import static org.junit.Assert.*;
import static pl.morgwai.base.guice.scopes.TestContexts.*;
import static pl.morgwai.base.guice.scopes.TrackableContext.executeWithinAll;



public class TrackableContextTests {



	final TestContext ctx1 = new TestContext(tracker);
	final SecondTestContext ctx2 = new SecondTestContext(secondTracker);
	final ThirdTestContext ctx3 = new ThirdTestContext(thirdTracker);
	final List<TrackableContext<?>> allCtxs = List.of(ctx1, ctx2, ctx3);

	static final String RESULT = "result";



	static ThrowingComputation<String, RuntimeException, RuntimeException> newThrowingComputation(
			Runnable task) {
		return new ThrowingComputation<>() {
			@Override public String perform() {
				task.run();
				return RESULT;
			}
			@Override public String toString() {
				return task.toString();
			}
		};
	}



	@Test
	public void testExecuteWithinAllMultipleCtxs() {
		final Runnable task = () -> {
			assertSame("ctx1 should be active", ctx1, tracker.getCurrentContext());
			assertSame("ctx2 should be active", ctx2, secondTracker.getCurrentContext());
			assertSame("ctx3 should be active", ctx3, thirdTracker.getCurrentContext());
		};
		executeWithinAll(allCtxs, task);
		assertSame(
			"result of executeWithinAll(...) should match the one returned by the passed task",
			RESULT,
			executeWithinAll(allCtxs, newThrowingComputation(task))
		);
	}



	@Test
	public void testExecuteWithinAllSingleCtx() {
		final Runnable task = () -> {
			assertSame("ctx1 should be active", ctx1, tracker.getCurrentContext());
			assertNull("ctx2 should not be active", secondTracker.getCurrentContext());
			assertNull("ctx3 should not be active", thirdTracker.getCurrentContext());
		};
		executeWithinAll(List.of(ctx1), task);
		assertSame(
			"result of executeWithinAll(...) should match the one returned by the passed task",
			RESULT,
			executeWithinAll(List.of(ctx1), newThrowingComputation(task))
		);
	}



	@Test
	public void testExecuteWithinAllNoCtxs() {
		final var logLevelBackup = trackableCtxLogger.getLevel();
		trackableCtxLogger.setLevel(Level.OFF);
		try {
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
			assertSame(
				"result of executeWithinAll(...) should match the one returned by the passed task",
				RESULT,
				executeWithinAll(List.of(), newThrowingComputation(noCtxTestTask))
			);
		} finally {
			trackableCtxLogger.setLevel(logLevelBackup);
		}
	}

	static final Logger trackableCtxLogger = Logger.getLogger(TrackableContext.class.getName());



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

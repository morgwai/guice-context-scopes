// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;



public class ContextTrackingExecutorTest {



	final ContextTracker<TestContext1> tracker1 = new ContextTracker<>();
	final ContextTracker<TestContext2> tracker2 = new ContextTracker<>();
	final ContextTracker<TestContext3> tracker3 = new ContextTracker<>();
	final ContextTracker<?>[] allTrackers = {tracker1, tracker2, tracker3};

	final ContextTrackingExecutor executor = new ContextTrackingExecutor(
			"testExecutor", 4, allTrackers);



	@Test
	public void testGetActiveContexts() {
		final var ctx1 = new TestContext1(tracker1);
		final var ctx3 = new TestContext3(tracker3);
		ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(() -> {
				final var activeCtxs = ContextTrackingExecutor.getActiveContexts(allTrackers);
				assertEquals("there should be 2 active ctxs",  2, activeCtxs.size());
				assertTrue("ctx1 should be active", activeCtxs.contains(ctx1));
				assertTrue("ctx3 should be active", activeCtxs.contains(ctx3));
			})
		);
	}



	@Test
	public void testExecuteWithinAll() throws Exception {
		final var ctx1 = new TestContext1(tracker1);
		final var ctx2 = new TestContext2(tracker2);
		final var ctx3 = new TestContext3(tracker3);
		final var allCtxs = new ArrayList<ServerSideContext<?>>(3);
		allCtxs.add(ctx1);
		allCtxs.add(ctx2);
		allCtxs.add(ctx3);
		ContextTrackingExecutor.executeWithinAll(allCtxs, () -> {
			assertSame("ctx1 should be active", ctx1, tracker1.getCurrentContext());
			assertSame("ctx2 should be active", ctx2, tracker2.getCurrentContext());
			assertSame("ctx3 should be active", ctx3, tracker3.getCurrentContext());
			return null;
		});
		ContextTrackingExecutor.executeWithinAll(allCtxs, () -> {
			assertSame("ctx1 should be active", ctx1, tracker1.getCurrentContext());
			assertSame("ctx2 should be active", ctx2, tracker2.getCurrentContext());
			assertSame("ctx3 should be active", ctx3, tracker3.getCurrentContext());
		});
	}



	@Test
	public void testExecute() throws Exception {
		final AssertionError[] errorHolder = {null};
		final var ctx1 = new TestContext1(tracker1);
		final var ctx3 = new TestContext3(tracker3);
		var barrier = new CyclicBarrier(2);
		ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(() -> {
				executor.execute(() -> {
					try {
						assertSame("ctx1 should be active", ctx1, tracker1.getCurrentContext());
						assertSame("ctx3 should be active", ctx3, tracker3.getCurrentContext());
					} catch (AssertionError e) {
						errorHolder[0] = e;
					} finally {
						try {
							barrier.await();
						} catch (Exception e) {}
					}
				});
			})
		);
		barrier.await(500l, TimeUnit.MILLISECONDS);
		if (errorHolder[0] != null) throw errorHolder[0];
	}



	@Test
	public void testWrapTasks() throws Exception {
		final var ctx1 = new TestContext1(tracker1);
		final var ctx3 = new TestContext3(tracker3);
		final int numberOfTasks = 5;
		final var tasks = new ArrayList<Callable<Void>>(numberOfTasks);
		final boolean[] executed = new boolean[numberOfTasks];
		for (int i = 0; i < numberOfTasks; i++) {
			final Integer taskNumber = Integer.valueOf(i);
			tasks.add(() -> {
				final var activeCtxs = ContextTrackingExecutor.getActiveContexts(allTrackers);
				assertEquals("there should be 2 active ctxs",  2, activeCtxs.size());
				assertTrue("ctx1 should be active", activeCtxs.contains(ctx1));
				assertTrue("ctx3 should be active", activeCtxs.contains(ctx3));
				executed[taskNumber] = true;
				return null;
			});
		}

		List<?>[] resultHolder = {null};
		ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(() -> {
				resultHolder[0] = executor.wrapTasks(tasks);
			})
		);
		@SuppressWarnings("unchecked")
		List<Callable<Void>> result = (List<Callable<Void>>) resultHolder[0];

		assertEquals("", numberOfTasks, result.size());
		for (var task: result) task.call();
		for (int i = 0; i < numberOfTasks; i++) {
			assertTrue("task " + i + "should be executed", executed[i]);
		}
	}



	@Test
	public void testInvokeAll() throws Exception {
		final AssertionError[] errorHolder = {null};
		final var ctx1 = new TestContext1(tracker1);
		final var ctx3 = new TestContext3(tracker3);
		final int numberOfTasks = 5;
		final var tasks = new ArrayList<Callable<Void>>(numberOfTasks);
		final boolean[] executed = new boolean[numberOfTasks];
		for (int i = 0; i < numberOfTasks; i++) {
			final Integer taskNumber = Integer.valueOf(i);
			tasks.add(() -> {
				final var activeCtxs = ContextTrackingExecutor.getActiveContexts(allTrackers);
				try {
					assertEquals("there should be 2 active ctxs",  2, activeCtxs.size());
					assertTrue("ctx1 should be active", activeCtxs.contains(ctx1));
					assertTrue("ctx3 should be active", activeCtxs.contains(ctx3));
					synchronized (executed) {
						executed[taskNumber] = true;
					}
				} catch (AssertionError e) {
					errorHolder[0] = e;
				}
				return null;
			});
		}

		ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(() -> {
				executor.invokeAll(tasks);
				return null;
			})
		);

		for (int i = 0; i < numberOfTasks; i++) {
			assertTrue("task " + i + "should be executed", executed[i]);
		}
		if (errorHolder[0] != null) throw errorHolder[0];
	}



	@After
	public void shutdown() {
		Logger.getLogger(ContextTrackingExecutor.class.getName()).setLevel(Level.WARNING);
		executor.tryShutdownGracefully(1);
	}



	static class TestContext1 extends ServerSideContext<TestContext1> {
		TestContext1(ContextTracker<TestContext1> tracker) { super(tracker); }
	}
	static class TestContext2 extends ServerSideContext<TestContext2> {
		TestContext2(ContextTracker<TestContext2> tracker) { super(tracker); }
	}
	static class TestContext3 extends ServerSideContext<TestContext3> {
		TestContext3(ContextTracker<TestContext3> tracker) { super(tracker); }
	}
}

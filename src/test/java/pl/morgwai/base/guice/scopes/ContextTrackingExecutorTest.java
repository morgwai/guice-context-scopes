// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
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

	static final int POOL_SIZE = 2;
	static final int QUEUE_SIZE = 1;
	final ContextTrackingExecutor executor = new ContextTrackingExecutor(
			"testExecutor", POOL_SIZE, new LinkedBlockingQueue<>(QUEUE_SIZE), allTrackers);



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
	public void testExecuteCallable() throws Exception {
		final AssertionError[] errorHolder = {null};
		final var ctx1 = new TestContext1(tracker1);
		final var ctx3 = new TestContext3(tracker3);
		String result = "result";
		var callFuture = ctx1.executeWithinSelf(
			() -> ctx3.executeWithinSelf(() -> {
				return executor.execute(() -> {
					try {
						assertSame("ctx1 should be active", ctx1, tracker1.getCurrentContext());
						assertSame("ctx3 should be active", ctx3, tracker3.getCurrentContext());
						return result;
					} catch (AssertionError e) {
						errorHolder[0] = e;
						throw e;
					}
				});
			})
		);
		assertSame("result should match", result, callFuture.get(500l, TimeUnit.MILLISECONDS));
		if (errorHolder[0] != null) throw errorHolder[0];
	}



	@Test
	public void testExecutionRejection() throws Exception {
		final var ctx1 = new TestContext1(tracker1);
		var barrier = new CyclicBarrier(POOL_SIZE + 1);
		ctx1.executeWithinSelf(() -> {
			for (int i = 0; i < POOL_SIZE; i++) {
				executor.execute(() -> {
					try {
						barrier.await(500l, TimeUnit.MILLISECONDS);
					} catch (Exception e) {}
				});
			}
			for (int i = 0; i < QUEUE_SIZE; i++) executor.execute(() -> {});
			try {
				executor.execute(() -> {});
				fail("RejectedExecutionException should be thrown");
			} catch (RejectedExecutionException e) {  // expected
			} finally {
				barrier.await(500l, TimeUnit.MILLISECONDS);
			}
			return null;
		});
	}



	@Test
	public void testCallableExecutionRejection() throws Exception {
		final var ctx1 = new TestContext1(tracker1);
		var barrier = new CyclicBarrier(POOL_SIZE + 1);
		ctx1.executeWithinSelf(() -> {
			for (int i = 0; i < POOL_SIZE; i++) {
				executor.execute(() -> {
					try {
						barrier.await(500l, TimeUnit.MILLISECONDS);
					} catch (Exception e) {}
				});
			}
			for (int i = 0; i < QUEUE_SIZE; i++) executor.execute(() -> {});
			try {
				executor.execute(() -> "result");
				fail("RejectedExecutionException should be thrown");
			} catch (RejectedExecutionException e) {  // expected
			} finally {
				barrier.await(500l, TimeUnit.MILLISECONDS);
			}
			return null;
		});
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

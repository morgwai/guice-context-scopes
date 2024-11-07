// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static pl.morgwai.base.guice.scopes.TestContexts.*;



public class ContextBinderTests {



	static final String RESULT = "result";

	final ContextBinder testSubject = new ContextBinder(List.of(tracker));
	final TestContext ctx = new TestContext(tracker);



	@Test
	public void testBindingRunnable() {
		final Runnable runnableToBind =
			() -> assertSame("Runnable callback should be bound to ctx",
					ctx, tracker.getCurrentContext());
		final var callback = ctx.executeWithinSelf(
				() -> testSubject.bindToContext(runnableToBind));
		assertNull("sanity check", tracker.getCurrentContext());
		callback.run();
	}



	@Test
	public void testBindingCallable() throws Exception {
		final Callable<String> callableToBind = () -> {
			assertSame("Callable callback should be bound to ctx",
					ctx, tracker.getCurrentContext());
			return RESULT;
		};
		final var callback = ctx.executeWithinSelf(
				() -> testSubject.bindToContext(callableToBind));
		assertNull("sanity check", tracker.getCurrentContext());
		assertSame("result should match",
				RESULT, callback.call());
	}



	@Test
	public void testBindingThrowingTask() {
		final var callback = ctx.executeWithinSelf(
			() -> testSubject.bindToContext(
				() -> assertSame("ThrowingTask callback should be bound to ctx",
						ctx, tracker.getCurrentContext())
			)
		);
		assertNull("sanity check", tracker.getCurrentContext());
		callback.execute();
	}



	@Test
	public void testBindingThrowingComputation() {
		final var callback = ctx.executeWithinSelf(
			() -> testSubject.bindToContext(
				() -> {
					assertSame("ThrowingTask callback should be bound to ctx",
							ctx, tracker.getCurrentContext());
					return RESULT;
				}
			)
		);
		assertNull("sanity check", tracker.getCurrentContext());
		assertSame("result should match",
				RESULT, callback.perform());
	}



	@Test
	public void testBindingConsumer() {
		final var callback = ctx.executeWithinSelf(
			() -> testSubject.bindToContext(
				(p) -> {
					assertSame("Consumer callback should be bound to ctx",
							ctx, tracker.getCurrentContext());
				}
			)
		);
		assertNull("sanity check", tracker.getCurrentContext());
		callback.accept(null);
	}



	@Test
	public void testBindingBiConsumer() {
		final var callback = ctx.executeWithinSelf(
			() -> testSubject.bindToContext(
				(p1, p2) -> {
					assertSame("BiConsumer callback should be bound to ctx",
							ctx, tracker.getCurrentContext());
				}
			)
		);
		assertNull("sanity check", tracker.getCurrentContext());
		callback.accept(null, null);
	}



	@Test
	public void testBindingFunction() {
		final var callback = ctx.executeWithinSelf(
			() -> testSubject.bindToContext(
				(p) -> {
					assertSame("Function callback should be bound to ctx",
							ctx, tracker.getCurrentContext());
					return RESULT;
				}
			)
		);
		assertNull("sanity check", tracker.getCurrentContext());
		assertSame("result should match",
				RESULT, callback.apply(null));
	}



	@Test
	public void testBindingBiFunction() {
		final var callback = ctx.executeWithinSelf(
			() -> testSubject.bindToContext(
				(p1, p2) -> {
					assertSame("BiFunction callback should be bound to ctx",
							ctx, tracker.getCurrentContext());
					return RESULT;
				}
			)
		);
		assertNull("sanity check", tracker.getCurrentContext());
		assertSame("result should match",
				RESULT, callback.apply(null, null));
	}



	@Test
	public void testBindingSupplier() {
		final var callback = ctx.executeWithinSelf(
			() -> testSubject.bindSupplierToContext(
				() -> {
					assertSame("Supplier callback should be bound to ctx",
							ctx, tracker.getCurrentContext());
					return RESULT;
				}
			)
		);
		assertNull("sanity check", tracker.getCurrentContext());
		assertSame("result should match",
				RESULT, callback.get());
	}
}

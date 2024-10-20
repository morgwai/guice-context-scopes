// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.*;
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
		final Runnable[] callbackHolder = {null};
		ctx.executeWithinSelf(() -> {
			callbackHolder[0] = testSubject.bindToContext(
				() -> assertSame("runnable should be bound to ctx",
						ctx, tracker.getCurrentContext())
			);
		});
		assertNull("sanity check", tracker.getCurrentContext());
		callbackHolder[0].run();
	}



	@Test
	public void testBindingConsumer() {
		final Consumer<?>[] callbackHolder = {null};
		ctx.executeWithinSelf(() -> {
			callbackHolder[0] = testSubject.bindToContext(
				(p) -> {
					assertSame("consumer should be bound to ctx",
							ctx, tracker.getCurrentContext());
				}
			);
		});
		assertNull("sanity check", tracker.getCurrentContext());
		callbackHolder[0].accept(null);
	}



	@Test
	public void testBindingBiConsumer() {
		final BiConsumer<?, ?>[] callbackHolder = {null};
		ctx.executeWithinSelf(() -> {
			callbackHolder[0] = testSubject.bindToContext(
				(p1, p2) -> {
					assertSame("biConsumer should be bound to ctx",
							ctx, tracker.getCurrentContext());
				}
			);
		});
		assertNull("sanity check", tracker.getCurrentContext());
		callbackHolder[0].accept(null, null);
	}



	@Test
	public void testBindingCallable() throws Exception {
		final Callable<?>[] callbackHolder = {null};
		ctx.executeWithinSelf(() -> {
			callbackHolder[0] = testSubject.bindToContext(
				() -> {
					assertSame("callable should be bound to ctx",
							ctx, tracker.getCurrentContext());
					return RESULT;
				}
			);
		});
		assertNull("sanity check", tracker.getCurrentContext());
		assertSame("result should match",
				RESULT, callbackHolder[0].call());
	}



	@Test
	public void testBindingFunction() {
		final Function<?, ?>[] callbackHolder = {null};
		ctx.executeWithinSelf(() -> {
			callbackHolder[0] = testSubject.bindToContext(
				(p) -> {
					assertSame("function should be bound to ctx",
							ctx, tracker.getCurrentContext());
					return RESULT;
				}
			);
		});
		assertNull("sanity check", tracker.getCurrentContext());
		assertSame("result should match",
				RESULT, callbackHolder[0].apply(null));
	}



	@Test
	public void testBindingBiFunction() {
		final BiFunction<?, ?, ?>[] callbackHolder = {null};
		ctx.executeWithinSelf(() -> {
			callbackHolder[0] = testSubject.bindToContext(
				(p1, p2) -> {
					assertSame("biFunction should be bound to ctx",
							ctx, tracker.getCurrentContext());
					return RESULT;
				}
			);
		});
		assertNull("sanity check", tracker.getCurrentContext());
		assertSame("result should match",
				RESULT, callbackHolder[0].apply(null, null));
	}



	@Test
	public void testBindingSupplier() {
		final Supplier<?>[] callbackHolder = {null};
		ctx.executeWithinSelf(() -> {
			callbackHolder[0] = testSubject.bindSupplierToContext(
				() -> {
					assertSame("callable should be bound to ctx",
							ctx, tracker.getCurrentContext());
					return RESULT;
				}
			);
		});
		assertNull("sanity check", tracker.getCurrentContext());
		assertSame("result should match",
				RESULT, callbackHolder[0].get());
	}
}

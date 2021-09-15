// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.Key;
import com.google.inject.Provider;
import org.junit.Test;

import static org.junit.Assert.*;



public class ContextScopeTest {



	final ContextTracker<TestContext> tracker = new ContextTracker<>();

	final ContextScope<TestContext> scope = new ContextScope<>("testScope", tracker);

	int sequence = 0;
	final Provider<Integer> provider = () -> ++sequence;
	final Key<Integer> key = Key.get(Integer.class);



	@Test
	public void testScoping() {
		final var ctx = new TestContext(tracker);
		ctx.executeWithinSelf(() -> {
			final var scoped = scope.scope(key, provider);
			final Integer scopedInt = scoped.get();
			assertSame("scoped value should remain the same", scopedInt, scoped.get());
			assertNotEquals("unscoped provider should provide a new value",
					scopedInt, provider.get());
		});
	}



	@Test
	public void testOutOfCtxScopingThrows() {
		try {
			scope.scope(key, provider).get();
			fail("scoping outside of any context should throw a RuntimeException");
		} catch (RuntimeException e) {}
	}



	@Test
	public void testRemoveFromScope() {
		final var ctx = new TestContext(tracker);
		ctx.executeWithinSelf(() -> {
			final var scoped = scope.scope(key, provider);
			final Integer scopedInt = scoped.get();
			ctx.removeAttribute(key);
			final Integer newScopedInt = scoped.get();
			assertNotEquals("after removing, scoped provider should provide a new value",
					scopedInt, newScopedInt);
			assertSame("the new scoped value should remain the same", newScopedInt, scoped.get());
		});
	}



	static class TestContext extends ServerSideContext<TestContext> {
		TestContext(ContextTracker<TestContext> tracker) { super(tracker); }
	}
}

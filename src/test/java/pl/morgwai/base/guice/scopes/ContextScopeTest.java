// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.*;
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
		new TestContext(tracker).executeWithinSelf(
			() -> {
				final var scopedProvider = scope.scope(key, provider);
				final var scopedInt = scopedProvider.get();
				assertSame("scoped value should remain the same", scopedInt, scopedProvider.get());
				assertNotEquals("unscoped provider should provide a new value",
						scopedInt, provider.get());
			}
		);
	}



	@Test
	public void testOutOfCtxScopingThrows() {
		try {
			scope.scope(key, provider).get();
			fail("scoping outside of any context should throw a OutOfScopeException");
		} catch (OutOfScopeException expected) {}
	}



	@Test
	public void testRemoveFromScope() {
		new TestContext(tracker).executeWithinSelf(
			() -> {
				final var scopedProvider = scope.scope(key, provider);
				final var oldScopedInt = scopedProvider.get();
				tracker.getCurrentContext().removeScopedObject(key);
				final var newScopedInt = scopedProvider.get();
				assertNotEquals("after removing, scoped provider should provide a new value",
						oldScopedInt, newScopedInt);
				assertSame("the new scoped value should remain the same",
						newScopedInt, scopedProvider.get());
			}
		);
	}



	static class TestContext extends TrackableContext<TestContext> {
		TestContext(ContextTracker<TestContext> tracker) { super(tracker); }
	}
}

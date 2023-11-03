// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.*;
import org.junit.Test;

import static org.junit.Assert.*;



public class ContextScopeTests {



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
				assertSame("scoped provider should keep providing the same object in a given ctx",
						scopedInt, scopedProvider.get());
				final var unscopedInt = provider.get();
				assertNotEquals("unscoped provider should provide an object different than scoped",
						scopedInt, unscopedInt);
				assertNotEquals("unscoped provider should provide a new object each time",
						unscopedInt, provider.get());
			}
		);
	}



	@Test
	public void testOutOfCtxScopingThrows() {
		try {
			scope.scope(key, provider).get();
			fail("scoping outside of any context should throw an OutOfScopeException");
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
				assertNotEquals("after removing, a scoped provider should provide a new object",
						oldScopedInt, newScopedInt);
				assertSame("the new scoped object should remain the same on subsequent calls",
						newScopedInt, scopedProvider.get());
			}
		);
	}



	static class TestContext extends TrackableContext<TestContext> {
		TestContext(ContextTracker<TestContext> tracker) { super(tracker, true); }
	}
}

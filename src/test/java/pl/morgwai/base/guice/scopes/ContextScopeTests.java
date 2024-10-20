// Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import org.junit.Test;

import com.google.inject.*;

import static org.junit.Assert.*;
import static pl.morgwai.base.guice.scopes.TestContexts.*;



public class ContextScopeTests {



	static final Key<Integer> INT_KEY = Key.get(Integer.class);

	final ContextScope<TestContext> scope = new ContextScope<>("testScope", tracker);

	int sequence = 0;
	final Provider<Integer> producer = new Provider<>() {
		@Override public Integer get() {
			return ++sequence;
		}
		@Override public String toString() {
			return "producer";
		}
	};



	@Test
	public void testScoping() {
		new TestContext(tracker).executeWithinSelf(
			() -> {
				final var scopedProvider = scope.scope(INT_KEY, producer);
				final var scopedInt = scopedProvider.get();
				assertSame("scopedProvider should keep providing the same object in a given ctx",
						scopedInt, scopedProvider.get());
				final var unscopedInt = producer.get();
				assertNotEquals("producer should provide an object different than scoped",
						scopedInt, unscopedInt);
				assertNotEquals("producer should provide a new object each time",
						unscopedInt, producer.get());
			}
		);
	}



	@Test
	public void testOutOfCtxScopingThrows() {
		try {
			scope.scope(INT_KEY, producer).get();
			fail("provisioning outside of any context should throw an OutOfScopeException");
		} catch (OutOfScopeException expected) {}
	}



	@Test
	public void testRemoveFromScope() {
		new TestContext(tracker).executeWithinSelf(
			() -> {
				final var scopedProvider = scope.scope(INT_KEY, producer);
				final var oldScopedInt = scopedProvider.get();
				tracker.getCurrentContext().removeScopedObject(INT_KEY);
				final var newScopedInt = scopedProvider.get();
				assertNotEquals("after removing, scopedProvider should provide a new object",
						oldScopedInt, newScopedInt);
				assertSame("the new scoped object should remain the same on subsequent calls",
						newScopedInt, scopedProvider.get());
			}
		);
	}



	@Test
	public void testScopedProviderToString() {
		assertTrue("scopedProvider.toString() should contain result of producer.toString()",
				scope.scope(INT_KEY, producer).toString().contains(producer.toString()));
	}
}

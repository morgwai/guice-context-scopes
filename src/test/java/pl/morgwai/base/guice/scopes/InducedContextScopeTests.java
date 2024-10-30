// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.Key;
import com.google.inject.Provider;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static pl.morgwai.base.guice.scopes.TestContexts.*;



public class InducedContextScopeTests {



	static final Key<Integer> INT_KEY = Key.get(Integer.class);

	final ContextTracker<TestContext> tracker = new ContextTracker<>();
	final ContextScope<TestContext> baseScope = new ContextScope<>("baseScope", tracker);
	final InducedContextScope<TestContext, InducedContext> inducedScope =
			new InducedContextScope<>("inducedScope", tracker, TestContext::getInducedCtx);

	int sequence = 0;
	final Provider<Integer> producer = () -> ++sequence;



	@Test
	public void testScoping() {
		final var inducedCtx = new InducedContext();
		final var inducedScopedIntHolder = new Integer[1];
		final var baseScopedIntHolder = new Integer[1];
		final var baseScopedProvider = baseScope.scope(INT_KEY, producer);
		new TestContext(tracker, inducedCtx).executeWithinSelf(
			() -> {
				final var inducedScopedProvider = inducedScope.scope(INT_KEY, producer);
				inducedScopedIntHolder[0] = inducedScopedProvider.get();
				baseScopedIntHolder[0] = baseScopedProvider.get();
				assertSame(
					"inducedScopedProvider should keep providing the same object within "
							+ "its base TestContext",
					inducedScopedIntHolder[0],
					inducedScopedProvider.get()
				);
				assertSame(
					"baseScopedProvider should keep providing the same object within "
							+ "a given base TestContext",
					baseScopedIntHolder[0],
					baseScopedProvider.get()
				);
				assertNotEquals(
					"base- and induced- scoped providers should provide different objects",
					baseScopedIntHolder[0],
					inducedScopedIntHolder[0]
				);
			}
		);
		new TestContext(tracker, inducedCtx).executeWithinSelf(
			() -> {
				final var inducedScopedProvider = inducedScope.scope(INT_KEY, producer);
				assertSame(
					"inducedScopedProvider should keep providing the same object within "
							+ "a given InducedContext across all its base TestContexts",
					inducedScopedIntHolder[0],
					inducedScopedProvider.get()
				);
				assertNotEquals(
					"baseScopedProvider should provide a new object in a new base TestContext",
					baseScopedIntHolder[0],
					baseScopedProvider.get()
				);
			}
		);
	}
}

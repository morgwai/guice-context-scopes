// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.Key;
import com.google.inject.Provider;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;



public class InducedContextScopeTests {



	static final Key<Integer> INT_KEY = Key.get(Integer.class);

	final ContextTracker<BaseContext> tracker = new ContextTracker<>();
	final ContextScope<BaseContext> baseScope = new ContextScope<>("baseScope", tracker);
	final InducedContextScope<BaseContext, InducedContext> inducedScope =
			new InducedContextScope<>("inducedScope", tracker, BaseContext::getInducedCtx);

	int sequence = 0;
	final Provider<Integer> producer = () -> ++sequence;



	@Test
	public void testScoping() {
		final var parentCtx = new InducedContext();
		final var parentScopedIntHolder = new Integer[1];
		final var childScopedIntHolder = new Integer[1];
		final var childScopedProvider = baseScope.scope(INT_KEY, producer);
		new BaseContext(tracker, parentCtx).executeWithinSelf(
			() -> {
				final var parentScopedProvider = inducedScope.scope(INT_KEY, producer);
				parentScopedIntHolder[0] = parentScopedProvider.get();
				childScopedIntHolder[0] = childScopedProvider.get();
				assertSame(
					"parent scoped provider should keep providing the same object in a child ctx",
					parentScopedIntHolder[0],
					parentScopedProvider.get()
				);
				assertSame(
					"child scoped provider should keep providing the same object in a child ctx",
					childScopedIntHolder[0],
					childScopedProvider.get()
				);
				assertNotEquals(
					"parent and child scoped providers should provide different objects",
					childScopedIntHolder[0],
					parentScopedIntHolder[0]
				);
			}
		);
		new BaseContext(tracker, parentCtx).executeWithinSelf(
			() -> {
				final var parentScopedProvider = inducedScope.scope(INT_KEY, producer);
				assertSame(
					"parent scoped provider should keep providing the same object in a given"
								+ " parent ctx across all its child ctxs",
					parentScopedIntHolder[0],
					parentScopedProvider.get()
				);
				assertNotEquals(
					"child scoped provider should provide a new object in a new child ctx",
					childScopedIntHolder[0],
					childScopedProvider.get()
				);
			}
		);
	}



	static class InducedContext extends InjectionContext {}

	static class BaseContext extends TrackableContext<BaseContext> {

		InducedContext getInducedCtx() { return inducedCtx; }
		final InducedContext inducedCtx;

		BaseContext(ContextTracker<BaseContext> tracker, InducedContext inducedCtx) {
			super(tracker);
			this.inducedCtx = inducedCtx;
		}
	}
}

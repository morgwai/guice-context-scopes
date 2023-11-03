// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.Key;
import com.google.inject.Provider;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;



public class InducedContextScopeTests {



	final ContextTracker<ChildContext> tracker = new ContextTracker<>();

	final ContextScope<ChildContext> childScope = new ContextScope<>("childScope", tracker);

	final InducedContextScope<ChildContext, InducedParentContext> parentScope =
			new InducedContextScope<>("inducedParentScope", tracker, ChildContext::getParentCtx);

	int sequence = 0;
	final Provider<Integer> provider = () -> ++sequence;
	final Key<Integer> key = Key.get(Integer.class);



	@Test
	public void testScoping() {
		final var parentCtx = new InducedParentContext();
		final var parentScopedIntHolder = new Integer[1];
		final var childScopedIntHolder = new Integer[1];
		final var childScopedProvider = childScope.scope(key, provider);
		new ChildContext(tracker, parentCtx).executeWithinSelf(
			() -> {
				final var parentScopedProvider = parentScope.scope(key, provider);
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
		new ChildContext(tracker, parentCtx).executeWithinSelf(
			() -> {
				final var parentScopedProvider = parentScope.scope(key, provider);
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



	static class InducedParentContext extends InjectionContext {
		InducedParentContext() { super(true); }
	}

	static class ChildContext extends TrackableContext<ChildContext> {

		InducedParentContext getParentCtx() { return parentCtx; }
		final InducedParentContext parentCtx;

		ChildContext(ContextTracker<ChildContext> tracker, InducedParentContext parentCtx) {
			super(tracker, true);
			this.parentCtx = parentCtx;
		}
	}
}

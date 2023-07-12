// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.Key;
import com.google.inject.Provider;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;



public class InducedContextScopeTest {



	final ContextTracker<ChildContext> tracker = new ContextTracker<>();

	final InducedContextScope<ChildContext, InducedParentContext> parentScope =
			new InducedContextScope<>("inducedScope", tracker, ChildContext::getParentCtx);

	int sequence = 0;
	final Provider<Integer> provider = () -> ++sequence;
	final Key<Integer> key = Key.get(Integer.class);



	@Test
	public void testScoping() {
		final var parentCtx = new InducedParentContext();
		final var scopedIntHolder = new Integer[1];
		final var unscopedIntHolder = new Integer[1];
		new ChildContext(tracker, parentCtx).executeWithinSelf(
			() -> {
				final var parentScopedProvider = parentScope.scope(key, provider);
				scopedIntHolder[0] = parentScopedProvider.get();
				assertSame(
					"parent scoped provider should keep providing the same object in a child ctx",
					scopedIntHolder[0],
					parentScopedProvider.get()
				);
				unscopedIntHolder[0] = provider.get();
				assertNotEquals("unscoped provider should provide an object different than scoped",
						scopedIntHolder[0], unscopedIntHolder[0]);
			}
		);
		new ChildContext(tracker, parentCtx).executeWithinSelf(
			() -> {
				final var parentScopedProvider = parentScope.scope(key, provider);
				assertSame(
					"parent scoped provider should keep providing the same object in a given"
								+ " parent ctx across all its child ctxs",
					scopedIntHolder[0],
					parentScopedProvider.get()
				);
				final var secondUnscopedInt = provider.get();
				assertNotEquals("unscoped provider should provide an object different than scoped",
						scopedIntHolder[0], secondUnscopedInt);
				assertNotEquals("unscoped provider should provide a new object each time",
						unscopedIntHolder[0], secondUnscopedInt);
			}
		);
	}



	static class InducedParentContext extends InjectionContext {}

	static class ChildContext extends TrackableContext<ChildContext> {

		InducedParentContext getParentCtx() { return parentCtx; }
		final InducedParentContext parentCtx;

		ChildContext(ContextTracker<ChildContext> tracker, InducedParentContext parentCtx) {
			super(tracker);
			this.parentCtx = parentCtx;
		}
	}
}

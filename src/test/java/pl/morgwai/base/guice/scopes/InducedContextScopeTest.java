// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.Key;
import com.google.inject.Provider;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;



public class InducedContextScopeTest {



	final ContextTracker<ChildContext> tracker = new ContextTracker<>();

	final InducedContextScope<ChildContext, InducedParentContext> scope =
			new InducedContextScope<>("inducedScope", tracker, ChildContext::getParentCtx);

	int sequence = 0;
	final Provider<Integer> provider = () -> ++sequence;
	final Key<Integer> key = Key.get(Integer.class);



	@Test
	public void testScoping() {
		final var parentCtx = new InducedParentContext();
		final var scopedIntHolder = new Integer[1];
		new ChildContext(tracker, parentCtx).executeWithinSelf(
			() -> {
				final var scopedProvider = scope.scope(key, provider);
				scopedIntHolder[0] = scopedProvider.get();
				assertSame("scoped value should remain the same",
						scopedIntHolder[0], scopedProvider.get());
				assertNotEquals("unscoped provider should provide a new value",
						scopedIntHolder[0], provider.get());
			}
		);
		new ChildContext(tracker, parentCtx).executeWithinSelf(
			() -> {
				final var scopedProvider = scope.scope(key, provider);
				assertSame("scoped value should remain the same",
						scopedIntHolder[0], scopedProvider.get());
				assertNotEquals("unscoped provider should provide a new value",
						scopedIntHolder[0], provider.get());
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

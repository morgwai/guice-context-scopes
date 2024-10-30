// Copyright 2024 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;



public class TestContexts {



	public static class InducedContext extends InjectionContext {}



	public static class TestContext extends TrackableContext<TestContext> {

		public InducedContext getInducedCtx() { return inducedCtx; }
		final InducedContext inducedCtx;

		public TestContext(ContextTracker<TestContext> tracker, InducedContext inducedCtx) {
			super(tracker);
			this.inducedCtx = inducedCtx;
		}

		public TestContext(ContextTracker<TestContext> tracker) {
			this(tracker, null);
		}
	}



	public static class SecondTestContext extends TrackableContext<SecondTestContext> {
		SecondTestContext(ContextTracker<SecondTestContext> tracker) { super(tracker); }
	}



	public static class ThirdTestContext extends TrackableContext<ThirdTestContext> {
		ThirdTestContext(ContextTracker<ThirdTestContext> tracker) { super(tracker); }
	}


	public static final ContextTracker<TestContext> tracker = new ContextTracker<>();
	public static final ContextTracker<SecondTestContext> secondTracker = new ContextTracker<>();
	public static final ContextTracker<ThirdTestContext> thirdTracker = new ContextTracker<>();
	public static final List<ContextTracker<?>> allTrackers =
			List.of(tracker, secondTracker, thirdTracker);
}

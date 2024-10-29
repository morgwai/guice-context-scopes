// Copyright 2024 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.lang.reflect.Type;
import java.util.HashSet;
import org.junit.Test;

import com.google.inject.*;

import static org.junit.Assert.*;
import static pl.morgwai.base.guice.scopes.ContextScopesModule.ContextTrackerType;
import static pl.morgwai.base.guice.scopes.TestContexts.*;



public class ContextScopesModuleTests {



	public static class FirstTrackableContext extends TrackableContext<FirstTrackableContext> {
		public FirstTrackableContext(ContextTracker<FirstTrackableContext> tracker) {
			super(tracker);
		}
	}



	public static class InducedContext extends InjectionContext {}



	public static class SecondTrackableContext extends TrackableContext<SecondTrackableContext> {

		final InducedContext inducedCtx;
		public InducedContext getInducedCtx() { return inducedCtx; }

		public SecondTrackableContext(
			ContextTracker<SecondTrackableContext> tracker,
			InducedContext inducedCtx
		) {
			super(tracker);
			this.inducedCtx = inducedCtx;
		}
	}



	public static class TestModule extends ContextScopesModule {

		public final ContextScope<FirstTrackableContext> firstScope =
				newContextScope("firstScope", FirstTrackableContext.class);

		public final ContextScope<SecondTrackableContext> secondScope =
				newContextScope("secondScope", SecondTrackableContext.class);

		public final Scope inducedScope = newInducedContextScope(
			"inducedScope",
			InducedContext.class,
			secondScope.tracker,
			SecondTrackableContext::getInducedCtx
		);

		public final ContextBinder ctxBinder = newContextBinder();
	}



	final TestModule testSubject = new TestModule();
	final ContextTracker<FirstTrackableContext> firstTracker = testSubject.firstScope.tracker;
	final ContextTracker<SecondTrackableContext> secondTracker = testSubject.secondScope.tracker;
	final Injector injector = Guice.createInjector(testSubject);
	final InducedContext inducedCtx = new InducedContext();
	final FirstTrackableContext firstCtx = new FirstTrackableContext(firstTracker);
	final SecondTrackableContext secondCtx = new SecondTrackableContext(secondTracker, inducedCtx);



	@Test
	public void testContextProviders() {
		firstCtx.executeWithinSelf(
			() -> secondCtx.executeWithinSelf(
				() -> {
					assertSame("enclosing firstCtx should be provided",
							firstCtx, injector.getInstance(FirstTrackableContext.class));
					assertSame("enclosing secondCtx should be provided",
							secondCtx, injector.getInstance(SecondTrackableContext.class));
					assertSame("inducedCtx associated with the secondCtx should be provided",
							inducedCtx, injector.getInstance(InducedContext.class));
				}
			)
		);
	}



	public static class TestComponent {

		final ContextBinder ctxBinder;

		@Inject public TestComponent(ContextBinder ctxBinder) {
			this.ctxBinder = ctxBinder;
		}

		public Runnable bindToCtx(Runnable task) {
			return ctxBinder.bindToContext(task);
		}
	}



	@Test
	public void testInjectingContextBinder() throws Exception {
		testContextBinderWiring(injector.getInstance(TestComponent.class));
	}

	@Test
	public void testContextBinderFromModule() throws Exception {
		testContextBinderWiring(new TestComponent(testSubject.ctxBinder));
	}

	void testContextBinderWiring(TestComponent component) throws Exception {
		assertNotNull("an instance of ContextBinder should be properly injected",
				component.ctxBinder);
		final Runnable testTask = () -> {
			assertSame("testTask should be bound to firstCtx",
					firstCtx, firstTracker.getCurrentContext());
			assertSame("testTask should be bound to secondCtx",
					secondCtx, secondTracker.getCurrentContext());
		};
		firstCtx.executeWithinSelf(
			() -> secondCtx.executeWithinSelf(
				() -> component.bindToCtx(testTask)
			)
		).run();
	}



	HashSet<TestContext> testContextSet;

	@Test
	public void testParameterizedTypeImplEqualsAndHashCode() throws NoSuchFieldException {
		final var reflectiveTrackerType =
				TestContexts.class.getDeclaredField("tracker").getGenericType();
		final var testContextTrackerType = new ContextTrackerType(TestContext.class);
		assertEquals(reflectiveTrackerType, testContextTrackerType);
		assertEquals(testContextTrackerType, reflectiveTrackerType);
		assertEquals(reflectiveTrackerType.hashCode(), testContextTrackerType.hashCode());

		final var typeSet = new HashSet<Type>();
		typeSet.add(reflectiveTrackerType);
		assertTrue("reflectiveTrackerType in a HashSet should be found by testContextTrackerType",
				typeSet.remove(testContextTrackerType));
		typeSet.add(testContextTrackerType);
		assertTrue("testContextTrackerType in a HashSet should be found by reflectiveTrackerType",
				typeSet.remove(reflectiveTrackerType));

		final var secondTestContextTrackerType = new ContextTrackerType(SecondTestContext.class);
		assertNotEquals(reflectiveTrackerType, secondTestContextTrackerType);
		assertNotEquals(secondTestContextTrackerType, reflectiveTrackerType);
		assertNotEquals(testContextTrackerType, secondTestContextTrackerType);
		assertNotEquals(secondTestContextTrackerType, testContextTrackerType);
		assertNotEquals(testContextTrackerType.hashCode(), secondTestContextTrackerType.hashCode());

		final var reflectiveTestContextSetType =
					getClass().getDeclaredField("testContextSet").getGenericType();
		assertNotEquals(testContextTrackerType, reflectiveTestContextSetType);
		assertNotEquals(reflectiveTestContextSetType, testContextTrackerType);
		assertNotEquals(testContextTrackerType.hashCode(), reflectiveTestContextSetType.hashCode());
	}
}

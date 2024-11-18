// Copyright 2024 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

import com.google.inject.*;

import static org.junit.Assert.*;
import static pl.morgwai.base.guice.scopes.ScopeModule.ContextTrackerType;
import static pl.morgwai.base.guice.scopes.TestContexts.*;



public class ScopeModuleTests {



	public static class TestModule extends ScopeModule {

		public final ContextScope<TestContext> firstScope =
				newContextScope("firstScope", TestContext.class);

		public final ContextScope<SecondTestContext> secondScope =
				newContextScope("secondScope", SecondTestContext.class);

		public final Scope inducedScope = newInducedContextScope(
			"inducedScope",
			InducedContext.class,
			firstScope.tracker,
			TestContext::getInducedCtx
		);

		public final ContextBinder ctxBinder = newContextBinder();
	}



	final TestModule testSubject = new TestModule();
	final ContextTracker<TestContext> firstTracker = testSubject.firstScope.tracker;
	final ContextTracker<SecondTestContext> secondTracker = testSubject.secondScope.tracker;
	final Injector injector = Guice.createInjector(testSubject);
	final InducedContext inducedCtx = new InducedContext();
	final TestContext firstCtx = new TestContext(firstTracker, inducedCtx);
	final SecondTestContext secondCtx = new SecondTestContext(secondTracker);



	@Test
	public void testContextProviders() {
		TrackableContext.executeWithinAll(
			List.of(firstCtx, secondCtx),
			() -> {
				assertSame("enclosing firstCtx should be provided",
						firstCtx, injector.getInstance(TestContext.class));
				assertSame("enclosing secondCtx should be provided",
						secondCtx, injector.getInstance(SecondTestContext.class));
				assertSame("inducedCtx associated with the secondCtx should be provided",
						inducedCtx, injector.getInstance(InducedContext.class));
			}
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
	public void testInjectingContextBinder() {
		testContextBinderWiring(injector.getInstance(TestComponent.class));
	}

	@Test
	public void testContextBinderFromModule() {
		testContextBinderWiring(new TestComponent(testSubject.ctxBinder));
	}

	void testContextBinderWiring(TestComponent component) {
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
	public void testContextTrackerTypeEqualsAndHashCode() throws NoSuchFieldException {
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

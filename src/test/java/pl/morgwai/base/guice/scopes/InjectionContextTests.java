// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.*;
import org.junit.Test;

import static org.junit.Assert.*;



public class InjectionContextTests {



	public interface Husband {
		Wife getWife();
	}

	public static class WorkingHusband implements Husband {
		private final Wife wife;
		public Wife getWife() { return wife; }
		@Inject public WorkingHusband(Wife wife) { this.wife = wife; }
	}

	public static class Wife {
		private final Husband husband;
		public Husband getHusband() { return husband; }
		@Inject public Wife(Husband husband) { this.husband = husband; }
	}

	public static class FamilyContext extends InjectionContext {

		FamilyContext(boolean disableCircularProxies) {
			super(disableCircularProxies);
		}

		public Husband marryArgumentIfHusbandAbsent(Husband husbandCandidate) {
			return produceIfAbsent(Key.get(Husband.class), () -> husbandCandidate);
		}
	}



	public void testCircularProxies(boolean disableCircularProxies) {
		final var injector = Guice.createInjector((binder) -> {
			binder.bind(Husband.class).to(WorkingHusband.class);
			binder.bind(Wife.class);
		});
		final var realHusband = injector.getInstance(Husband.class);
		final var husbandProxy = realHusband.getWife().getHusband();
		assertTrue("sanity check", Scopes.isCircularProxy(husbandProxy));
		final var family = new FamilyContext(disableCircularProxies);
		assertSame("on paper, husbandProxy should seem like a family member",
				husbandProxy, family.marryArgumentIfHusbandAbsent(husbandProxy));
		if (disableCircularProxies) {
			assertSame("family should assume there are no proxies and honor the first marriage",
					husbandProxy, family.marryArgumentIfHusbandAbsent(realHusband));
		} else {
			assertNotSame("husbandProxy should not be a real member of family and when realHusband "
					+ "finally arrives, he should replace husbandProxy",
					husbandProxy, family.marryArgumentIfHusbandAbsent(realHusband));
		}
	}

	@Test
	public void testCircularProxiesDisabled() {
		testCircularProxies(true);
	}

	@Test
	public void testCircularProxiesEnabled() {
		testCircularProxies(false);
	}
}

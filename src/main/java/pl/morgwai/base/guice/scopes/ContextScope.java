// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;



/**
 * Scopes objects to a call context obtained from the associated {@link ContextTracker}.
 */
public class ContextScope<Ctx extends ServerSideContext<Ctx>> implements Scope {



	final ContextTracker<Ctx> tracker;

	final String name;
	public String getName() { return name; }



	@Override
	public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
		return () -> {
			final var ctx = tracker.getCurrentContext();
			if (ctx == null) {
				throw new RuntimeException("no context for thread "
						+ Thread.currentThread().getName() + " in scope " + name);
			}
			return ctx.provideAttributeIfAbsent(key, unscoped);
		};
	}



	@Override
	public String toString() {
		return name;
	}



	public ContextScope(String name, ContextTracker<Ctx> tracker) {
		this.name = name;
		this.tracker = tracker;
	}
}

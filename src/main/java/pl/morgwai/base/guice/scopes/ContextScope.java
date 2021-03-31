/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;



/**
 * Scopes objects to a context of a call obtained from the associated {@link ContextTracker}.
 */
public class ContextScope<Ctx extends ServerSideContext<Ctx>> implements Scope {



	ContextTracker<Ctx> tracker;

	String name;
	public String getName() { return name; }



	@Override
	public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
		return () -> {
			Ctx ctx = tracker.getCurrentContext();
			if (ctx == null) {
				throw new RuntimeException("no call context for this thread in scope " + name);
			}
			@SuppressWarnings("unchecked")
			T instance = (T) ctx.getAttribute(key);
			if (instance == null) {
				instance = unscoped.get();
				ctx.setAttribute(key, instance);
			}
			return instance;
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

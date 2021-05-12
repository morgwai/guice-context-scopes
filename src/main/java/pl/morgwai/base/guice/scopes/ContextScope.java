// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;



/**
 * Scopes objects to a context of a call obtained from the associated {@link ContextTracker}.<br/>
 * Scopes are thread-safe as long as context attributes that are accessed by several threads are not
 * accessed manually using {@link ServerSideContext}'s <code>{get,set,remove}Attribute(...)</code>
 * methods.
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
				throw new RuntimeException("no context for this thread in scope " + name);
			}
			synchronized (ctx) {
				@SuppressWarnings("unchecked")
				T instance = (T) ctx.getAttribute(key);
				if (instance == null) {
					instance = unscoped.get();
					ctx.setAttribute(key, instance);
				}
				return instance;
			}
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

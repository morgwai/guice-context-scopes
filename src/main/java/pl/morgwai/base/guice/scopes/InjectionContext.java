// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.inject.Key;
import com.google.inject.Provider;



/**
 * Stores objects scoped to the context of some processing/call/request/session, such as an RPC, a
 * servlet request processing, a session combining several calls etc. Stored objects can be obtained
 * via Guice injections configured with bindings scoped in the associated {@link ContextScope}.
 * <p>
 * Note: most context classes should rather override {@link TrackableContext} subclass instead of
 * this one. The main exception are contexts that are induced by another contexts: see
 * {@link InducedContextScope}.</p>
 * <p>
 * Overriding classes usually add properties and methods specific to a given type of call, like
 * given call's arguments etc.</p>
 * <p>
 * If multiple threads run within the same context, the attributes that they access must be
 * thread-safe or properly synchronized.</p>
 */
public abstract class InjectionContext {



	private final ConcurrentMap<Key<?>, Object> scopedObjects = new ConcurrentHashMap<>();



	/**
	 * Removes the object given by <code>key</code> from this context. This is sometimes useful
	 * to force the associated {@link ContextScope} to obtain a new instance from the unscoped
	 * provider if the current one is not usable anymore (for example a timed-out connection, etc).
	 * <p>
	 * <b>Note:</b> If multiple threads run within the same context, care must be taken to prevent
	 * some of them from retaining the old stale instance.</p>
	 */
	public void removeScopedObject(Key<?> key) {
		scopedObjects.remove(key);
	}



	/**
	 * Obtains the object given by {@code key}. If there is a stored instance in this context, it is
	 * returned immediately. Otherwise, a new instance is obtained from {@code provider} and stored
	 * for subsequent calls.
	 * <p>
	 * This method is intended for internal use by libs built on top of this one. Apps should obtain
	 * scoped objects via Guice injections.</p>
	 */
	@SuppressWarnings("unchecked")
	public <T> T provideIfAbsent(Key<T> key, Provider<T> provider) {
		return (T) scopedObjects.computeIfAbsent(key, (ignored) -> provider.get());
	}
}

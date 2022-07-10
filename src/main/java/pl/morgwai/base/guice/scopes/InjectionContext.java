// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.inject.Key;
import com.google.inject.Provider;



/**
 * Stores objects scoped to some processing/call/request/session, such as an RPC, a servlet
 * request processing, a session combining several calls etc. Stored objects can be obtained using
 * {@link Provider}s bound in the associated {@link ContextScope}.
 * <p>
 * Overriding classes usually add properties and methods specific to a given type of call, like
 * given call's arguments etc.</p>
 * <p>
 * If multiple threads run within the same context, the attributes that they access must be
 * thread-safe or properly synchronized.</p>
 * <p>
 * Note: most context classes should rather override {@link TrackableContext} subclass instead of
 * this one. The main exception are contexts that are induced by another contexts: see
 * {@link InducedContextScope}.</p>
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
	 * Obtains the object given by {@code key}. If it is not yet present in this context,
	 * asks {@code provider} for a new instance and stores the result for subsequent calls.
	 * For internal use by {@link ContextScope#scope(Key, Provider)}.
	 */
	@SuppressWarnings("unchecked")
	<T> T provideIfAbsent(Key<T> key, Provider<T> provider) {
		return (T) scopedObjects.computeIfAbsent(key, (ignored) -> provider.get());
	}
}

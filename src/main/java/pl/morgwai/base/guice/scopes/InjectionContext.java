// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.inject.Key;
import com.google.inject.Provider;



/**
 * Stores objects scoped to the context of some processing/call/request/session, such as an RPC, a
 * servlet request processing, a session combining several calls etc. Stored objects can be obtained
 * via Guice injections configured with bindings scoped in the associated {@link ContextScope}.
 * <p>
 * Note: most context classes should rather extend {@link TrackableContext} subclass instead of
 * this one. The main exception are context types that are induced by other contexts: see
 * {@link InducedContextScope}.</p>
 * <p>
 * Subclasses usually add properties and methods specific to their type, like
 * their call's arguments, a reference to their session object etc.</p>
 * <p>
 * Multiple threads may run within the same context, but the scoped objects that they access must be
 * thread-safe or properly synchronized.</p>
 */
public abstract class InjectionContext implements Serializable {



	private final ConcurrentMap<Key<?>, Object> scopedObjects = new ConcurrentHashMap<>();



	/**
	 * Removes the object given by <code>key</code> from this context. This is sometimes useful
	 * to force the associated {@link ContextScope} to obtain a new instance from the unscoped
	 * provider if the current one is not usable anymore (for example a timed-out connection, etc).
	 * <p>
	 * <b>Note:</b> If multiple threads run within the same context, care must be taken to prevent
	 * some of them from retaining the old stale instances.</p>
	 */
	public void removeScopedObject(Key<?> key) {
		scopedObjects.remove(key);
	}



	/**
	 * Obtains the object given by {@code key}. If there is a stored instance in this context, it is
	 * returned immediately. Otherwise, a new instance is obtained from {@code provider} and stored
	 * for subsequent calls.
	 */
	protected <T> T provideIfAbsent(Key<T> key, Provider<T> provider) {
		@SuppressWarnings("unchecked")
		final var result = (T) scopedObjects.computeIfAbsent(key, (ignored) -> provider.get());
		return result;
	}



	private static final long serialVersionUID = 5939049347722528201L;
}

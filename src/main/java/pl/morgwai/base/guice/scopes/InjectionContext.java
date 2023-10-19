// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.inject.Key;
import com.google.inject.Provider;



/**
 * Stores objects {@link com.google.inject.Scope scoped} to the context of some
 * processing/call/request/session, such as an RPC, a servlet request processing, a session
 * combining several calls etc. Each concrete subclass corresponds to a specific type of events and
 * each instance corresponds to a single such event. For example an instance of
 * {@code HttpRequestContext} may correspond to the processing of a single HTTP request. Creation of
 * the instance must be hooked at the beginning of a given processing: for example in Java Servlet
 * environment, a {@code HttpRequestContext} may be created in a {@code Filter}.
 * <p>
 * Note: most context classes should rather extend {@link TrackableContext} subclass instead of
 * this one. The main exception are context types that are
 * {@link InducedContextScope induced by other contexts}.</p>
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
	 * Removes the object given by {@code key} from this context. This forces retrieval of a new
	 * instance during the {@link #provideIfAbsent(Key, Provider) next provision} within the
	 * {@link ContextScope scope of this context}. This is useful if the currently stored instance
	 * is not usable anymore (for example a timed-out connection, expired token, etc).<br>
	 * If there's no object stored under {@code key} in this context, this method has no effect.
	 * <p>
	 * <b>Note:</b> If multiple threads run within the same context, care must be taken to prevent
	 * some of them from retaining the old stale instances.</p>
	 */
	public void removeScopedObject(Key<?> key) {
		scopedObjects.remove(key);
	}



	/**
	 * Provides the object given by {@code key}. If there already is an instance scoped to this
	 * context, it is returned immediately. Otherwise, a new instance is obtained
	 * from {@code provider} and stored for subsequent calls.
	 */
	protected <T> T provideIfAbsent(Key<T> key, Provider<T> provider) {
		@SuppressWarnings("unchecked")
		final var result = (T) scopedObjects.computeIfAbsent(key, (ignored) -> provider.get());
		return result;
	}



	private static final long serialVersionUID = 5939049347722528201L;
}

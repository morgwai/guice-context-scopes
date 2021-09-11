// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.inject.Key;
import com.google.inject.Provider;



/**
 * Stores attributes associated with some server-side processing/call (such as a servlet request
 * processing, an RPC or a session combining several received calls) and allows to execute
 * operations within itself.
 * <p>
 * Overriding classes must use themselves as {@code Ctx} type argument.</p>
 * <p>
 * Overriding classes usually add properties and methods specific to a given type of call, like
 * given call's arguments etc.</p>
 * <p>
 * If many threads run within the same context, the attributes that they access must be thread-safe
 * or properly synchronized.</p>
 */
public abstract class ServerSideContext<Ctx extends ServerSideContext<Ctx>> {



	final ConcurrentMap<Key<?>, Object> attributes = new ConcurrentHashMap<>();
	final ContextTracker<Ctx> tracker;



	protected ServerSideContext(ContextTracker<Ctx> tracker) {
		this.tracker = tracker;
	}



	@SuppressWarnings("unchecked")
	public void executeWithinSelf(Runnable operation) {
		tracker.setCurrentContext((Ctx) this);
		try {
			operation.run();
		} finally {
			tracker.clearCurrentContext();
		}
	}



	@SuppressWarnings("unchecked")
	public <T> T executeWithinSelf(Callable<T> operation) throws Exception {
		tracker.setCurrentContext((Ctx) this);
		try {
			return operation.call();
		} finally {
			tracker.clearCurrentContext();
		}
	}



	/**
	 * Removes the attribute given by <code>key</code> from this context. This is sometimes useful
	 * to force the associated {@link ContextScope} to obtain a new instance from the unscoped
	 * provider if the current one is not usable anymore (for example a timed-out connection, etc).
	 * <p>
	 * <b>Note:</b> If multiple threads run within the same context, care must be taken to prevent
	 * some of them from retaining the old stale instance.</p>
	 */
	public void removeAttribute(Key<?> key) {
		attributes.remove(key);
	}



	/**
	 * For internal use only by {@link ContextScope#scope(Key, com.google.inject.Provider)}.
	 */
	@SuppressWarnings("unchecked")
	<T> T provideAttributeIfAbsent(Key<T> key, Provider<T> provider) {
		return (T) attributes.computeIfAbsent(key, (ignored) -> provider.get());
	}
}

/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;



/**
 * Stores attributes associated with some server-side call (such as a servlet request or an RPC)
 * and allows to run tasks within itself.
 */
public abstract class ServerSideContext<Ctx extends ServerSideContext<Ctx>> {



	Map<Object, Object> attributes;
	ContextTracker<Ctx> tracker;



	@SuppressWarnings("unchecked")
	public void runWithinSelf(Runnable operation) {
		tracker.setCurrentContext((Ctx) this);
		operation.run();
		tracker.clearCurrentContext();
	}



	@SuppressWarnings("unchecked")
	public <T> T callWithinSelf(Callable<T> operation) throws Exception {
		tracker.setCurrentContext((Ctx) this);
		try {
			return operation.call();
		} finally {
			tracker.clearCurrentContext();
		}
	}



	public void setAttribute(Object key, Object attribute) {
		attributes.put(key, attribute);
	}



	public void removeAttribute(Object key) {
		attributes.remove(key);
	}



	public Object getAttribute(Object key) {
		return attributes.get(key);
	}



	protected ServerSideContext(ContextTracker<Ctx> tracker) {
		this.tracker = tracker;
		attributes = new HashMap<>();
	}
}

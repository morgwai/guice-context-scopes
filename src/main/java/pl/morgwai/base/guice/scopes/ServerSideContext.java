/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;



/**
 * Stores attributes associated with some server-side processing/call (such as a servlet request,
 * an RPC or a session combining several received calls) and allows to run operations within itself.
 */
public abstract class ServerSideContext<Ctx extends ServerSideContext<Ctx>> {



	Map<Object, Object> attributes;
	ContextTracker<Ctx> tracker;



	@SuppressWarnings("unchecked")
	public void runWithinSelf(Runnable operation) {
		tracker.setCurrentContext((Ctx) this);
		try {
			operation.run();
		} finally {
			tracker.clearCurrentContext();
		}
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



	public void setAttribute(Object key, Object attribute) { attributes.put(key, attribute); }

	public Object getAttribute(Object key) { return attributes.get(key); }

	public Object removeAttribute(Object key) { return attributes.remove(key); }



	protected ServerSideContext(ContextTracker<Ctx> tracker) {
		this.attributes = new HashMap<>();
		this.tracker = tracker;
	}
}

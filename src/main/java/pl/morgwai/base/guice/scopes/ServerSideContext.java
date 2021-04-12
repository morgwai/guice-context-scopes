/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;

import java.util.HashMap;
import java.util.Map;



/**
 * Stores attributes associated with some server-side processing/call, such as a servlet request,
 * an RPC or a session combining several received calls.
 */
public class ServerSideContext {



	Map<Object, Object> attributes;



	public void setAttribute(Object key, Object attribute) {
		attributes.put(key, attribute);
	}



	public Object removeAttribute(Object key) {
		return attributes.remove(key);
	}



	public Object getAttribute(Object key) {
		return attributes.get(key);
	}



	public ServerSideContext() {
		attributes = new HashMap<>();
	}
}

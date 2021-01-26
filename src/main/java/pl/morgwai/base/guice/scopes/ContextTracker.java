/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;



/**
 * Allows to track which server-side call is handled by which thread.
 */
public interface ContextTracker<Ctx extends ServerCallContext> {

	/**
	 * @return call context of the calling thread.
	 */
	Ctx getCurrentContext();

	/**
	 * Must be called whenever some server-side call gets dispatched to a new thread.
	 */
	void setCurrentContext(Ctx ctx);

	/**
	 * Should be called when a given thread finishes processing of a given call.
	 */
	void clearCurrentContext();
}

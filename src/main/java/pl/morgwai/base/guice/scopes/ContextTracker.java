// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;



/**
 * Allows to track which server-side call is handled by which thread.
 */
public class ContextTracker<Ctx extends ServerSideContext<Ctx>> {



	private final ThreadLocal<Ctx> currentContex = new ThreadLocal<>();



	/**
	 * @return context which the calling thread is running within.
	 */
	public Ctx getCurrentContext() {
		return currentContex.get();
	}



	/**
	 * Sets the context for the current thread to {@code ctx}.
	 * For internal use. Apps and deriving libs should rather use
	 * {@link ServerSideContext#executeWithinSelf(java.util.concurrent.Callable)}.
	 */
	void setCurrentContext(Ctx ctx) {
		currentContex.set(ctx);
	}



	/**
	 * Dissociates the current thread from the current context. Should be called to prevent
	 * retaining of otherwise unused attributes that can be garbage-collected.
	 * For internal use. Apps and deriving libs should rather use
	 * {@link ServerSideContext#executeWithinSelf(java.util.concurrent.Callable)}.
	 */
	void clearCurrentContext() {
		currentContex.remove();
	}
}

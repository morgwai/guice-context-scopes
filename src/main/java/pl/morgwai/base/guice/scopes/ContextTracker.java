// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;



/**
 * Allows to track which server-side call is handled by which thread.
 */
public class ContextTracker<Ctx extends ServerSideContext<Ctx>> {



	private final ThreadLocal<Ctx> currentContex = new ThreadLocal<>();



	/**
	 * @return call context of the calling thread.
	 */
	public Ctx getCurrentContext() {
		return currentContex.get();
	}

	/**
	 * For internal use. Apps and deriving libs should rather use
	 * {@link ServerSideContext#executeWithinSelf(Runnable)} and
	 * {@link ServerSideContext#executeWithinSelf(java.util.concurrent.Callable)}.
	 */
	void setCurrentContext(Ctx ctx) {
		currentContex.set(ctx);
	}

	/**
	 * For internal use. Apps and deriving libs should rather use
	 * {@link ServerSideContext#executeWithinSelf(Runnable)} and
	 * {@link ServerSideContext#executeWithinSelf(java.util.concurrent.Callable)}.
	 */
	void clearCurrentContext() {
		currentContex.remove();
	}
}

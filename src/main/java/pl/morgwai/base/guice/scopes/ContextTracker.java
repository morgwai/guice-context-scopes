/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;



/**
 * Allows to track which server-side call is handled by which thread.
 */
public abstract class ContextTracker<Ctx extends ServerSideContext<Ctx>> {

	/**
	 * @return call context of the calling thread.
	 */
	public abstract Ctx getCurrentContext();

	/**
	 * For internal use. Apps and deriving libs should rather use
	 * {@link ServerSideContext#runWithinSelf(Runnable)} and
	 * {@link ServerSideContext#callWithinSelf(java.util.concurrent.Callable)}.
	 */
	protected abstract void setCurrentContext(Ctx ctx);

	/**
	 * For internal use. Apps and deriving libs should rather use
	 * {@link ServerSideContext#runWithinSelf(Runnable)} and
	 * {@link ServerSideContext#callWithinSelf(java.util.concurrent.Callable)}.
	 */
	protected abstract void clearCurrentContext();
}

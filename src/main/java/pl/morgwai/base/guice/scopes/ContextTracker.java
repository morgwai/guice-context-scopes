/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;



/**
 * Allows to track which server-side call is handled by which thread.
 */
public class ContextTracker<Ctx extends ServerSideContext<Ctx>> {



	private final ThreadLocal<Ctx> contexts = new ThreadLocal<>();



	/**
	 * @return call context of the calling thread.
	 */
	public Ctx getCurrentContext() {
		return contexts.get();
	}

	/**
	 * For internal use. Apps and deriving libs should rather use
	 * {@link ServerSideContext#runWithinSelf(Runnable)} and
	 * {@link ServerSideContext#callWithinSelf(java.util.concurrent.Callable)}.
	 */
	void setCurrentContext(Ctx ctx) {
		contexts.set(ctx);
	}

	/**
	 * For internal use. Apps and deriving libs should rather use
	 * {@link ServerSideContext#runWithinSelf(Runnable)} and
	 * {@link ServerSideContext#callWithinSelf(java.util.concurrent.Callable)}.
	 */
	protected void clearCurrentContext() {
		contexts.remove();
	}
}

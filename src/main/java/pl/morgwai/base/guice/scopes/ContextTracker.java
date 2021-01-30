/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.Callable;



/**
 * Allows to track which server-side call is handled by which thread.
 */
public interface ContextTracker<Ctx extends ServerCallContext<Ctx>> {

	/**
	 * @return call context of the calling thread.
	 */
	Ctx getCurrentContext();

	/**
	 * Runs operation within a given context.
	 */
	void runWithin(Ctx ctx, Runnable operation);

	/**
	 * Calls operation within a given context.
	 */
	<T> T callWithin(Ctx ctx, Callable<T> operation) throws Exception;

	/**
	 * @deprecated use {@link #runWithin(ServerCallContext, Runnable)} or
	 * {@link #callWithin(ServerCallContext, Callable)}.
	 */
	@Deprecated
	void setCurrentContext(Ctx ctx);

	/**
	 * @deprecated use {@link #runWithin(ServerCallContext, Runnable)} or
	 * {@link #callWithin(ServerCallContext, Callable)}.
	 */
	@Deprecated
	void clearCurrentContext();
}

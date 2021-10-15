// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.Callable;



/**
 * Allows to track which server-side call is handled by which thread.
 */
public class ContextTracker<CtxT extends ServerSideContext<CtxT>> {



	private final ThreadLocal<CtxT> currentContext = new ThreadLocal<>();



	/**
	 * Returns context which the calling thread is running within.
	 *
	 * @see ContextTrackingExecutor#getActiveContexts(ContextTracker...)
	 */
	public CtxT getCurrentContext() {
		return currentContext.get();
	}



	/**
	 * For internal use by {@link ServerSideContext#executeWithinSelf(Runnable)}.
	 */
	void trackWhileExecuting(CtxT ctx, Runnable operation) {
		currentContext.set(ctx);
		try {
			operation.run();
		} finally {
			currentContext.remove();
		}
	}



	/**
	 * For internal use by {@link ServerSideContext#executeWithinSelf(Callable)}.
	 */
	<T> T trackWhileExecuting(CtxT ctx, Callable<T> operation) throws Exception {
		currentContext.set(ctx);
		try {
			return operation.call();
		} finally {
			currentContext.remove();
		}
	}
}

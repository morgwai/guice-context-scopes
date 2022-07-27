// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;



/**
 * Allows to track which thread is within which context.
 */
public class ContextTracker<CtxT extends TrackableContext<CtxT>> {



	private final ThreadLocal<CtxT> currentContext = new ThreadLocal<>();



	/**
	 * Returns context of the current thread.
	 *
	 * @see ContextTrackingExecutor#getActiveContexts(List)
	 */
	public CtxT getCurrentContext() {
		return currentContext.get();
	}



	/**
	 * For internal use by {@link TrackableContext#executeWithinSelf(Runnable)}.
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
	 * For internal use by {@link TrackableContext#executeWithinSelf(Callable)}.
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

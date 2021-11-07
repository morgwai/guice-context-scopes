// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.Callable;



/**
 * A {@link ServerSideContext} that can be tracked across threads by executing closures within it.
 * <p>
 * Overriding classes must use themselves as {@code CtxT} type argument.</p>
 */
public abstract class TrackableContext<CtxT extends TrackableContext<CtxT>>
		extends ServerSideContext {



	private final ContextTracker<CtxT> tracker;



	protected TrackableContext(ContextTracker<CtxT> tracker) {
		this.tracker = tracker;
	}



	/**
	 * Sets itself as the current context for the current thread and executes {@code operation}
	 * synchronously. Afterwards clears the current context.
	 *
	 * @see ContextTrackingExecutor#executeWithinAll(java.util.List, Runnable)
	 */
	@SuppressWarnings("unchecked")
	public void executeWithinSelf(Runnable operation) {
		tracker.trackWhileExecuting((CtxT) this, operation);
	}



	/**
	 * Sets itself as the current context for the current thread and executes {@code operation}
	 * synchronously. Afterwards clears the current context.
	 *
	 * @see ContextTrackingExecutor#executeWithinAll(java.util.List, Callable)
	 */
	@SuppressWarnings("unchecked")
	public <T> T executeWithinSelf(Callable<T> operation) throws Exception {
		return tracker.trackWhileExecuting((CtxT) this, operation);
	}
}

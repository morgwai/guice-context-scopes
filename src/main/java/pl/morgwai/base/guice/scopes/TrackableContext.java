// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.Callable;



/**
 * An {@link InjectionContext} that can {@link #executeWithinSelf(Runnable) execute operations
 * within itself}, so that it can be tracked across threads using the associated
 * {@link ContextTracker}.
 * <p>
 * Overriding classes must use themselves as {@code CtxT} type argument.</p>
 */
public abstract class TrackableContext<CtxT extends TrackableContext<CtxT>>
		extends InjectionContext {



	private final ContextTracker<CtxT> tracker;



	protected TrackableContext(ContextTracker<CtxT> tracker) {
		this.tracker = tracker;
	}



	/**
	 * Sets itself as the current context for the current thread and executes {@code task}
	 * synchronously. Afterwards clears the current context.
	 *
	 * @see ContextTrackingExecutor#executeWithinAll(java.util.List, Runnable)
	 */
	public void executeWithinSelf(Runnable task) {
		@SuppressWarnings("unchecked")
		final var thisCtx = (CtxT) this;
		try {
			tracker.trackWhileExecuting(
				thisCtx,
				new Callable<Void>() {

					@Override public Void call() {
						task.run();
						return null;
					}

					@Override public String toString() { return task.toString(); }
				}
			);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception ignored) {}  // dead code: result of wrapping task with a Callable
	}



	/**
	 * Sets itself as the current context for the current thread and executes {@code task}
	 * synchronously. Afterwards clears the current context.
	 *
	 * @see ContextTrackingExecutor#executeWithinAll(java.util.List, Callable)
	 */
	public <T> T executeWithinSelf(Callable<T> task) throws Exception {
		@SuppressWarnings("unchecked")
		final var thisCtx = (CtxT) this;
		return tracker.trackWhileExecuting(thisCtx, task);
	}
}

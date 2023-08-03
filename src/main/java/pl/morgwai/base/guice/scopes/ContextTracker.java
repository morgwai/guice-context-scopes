// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.*;
import java.util.concurrent.Callable;



/** Allows to track which thread is running within which context. */
public class ContextTracker<CtxT extends TrackableContext<CtxT>> {



	private final ThreadLocal<CtxT> currentContext = new ThreadLocal<>();



	/**
	 * Returns context of the current thread.
	 * @see #getActiveContexts(List)
	 */
	public CtxT getCurrentContext() {
		return currentContext.get();
	}



	/** For internal use by {@link TrackableContext#executeWithinSelf(Callable)}. */
	<T> T trackWhileExecuting(CtxT ctx, Callable<T> task) throws Exception {
		currentContext.set(ctx);
		try {
			return task.call();
		} finally {
			currentContext.remove();
		}
	}



	/**
	 * Retrieves all active contexts from {@code trackers}. The returned list can be then used
	 * as an argument to {@link TrackableContext#executeWithinAll(List, Runnable)} to transfer the
	 * contexts when switching to another thread.
	 * <p>
	 * Libraries usually bind {@code List<ContextTracker<?>>} to an instance containing all possible
	 * trackers for use as an argument for this method.</p>
	 */
	public static List<TrackableContext<?>> getActiveContexts(List<ContextTracker<?>> trackers) {
		final var activeContexts = new ArrayList<TrackableContext<?>>(trackers.size());
		for (var tracker: trackers) {
			final var ctx = tracker.getCurrentContext();
			if (ctx != null) activeContexts.add(ctx);
		}
		return activeContexts;
	}
}

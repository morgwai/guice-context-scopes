// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.*;
import java.util.concurrent.Callable;



/**
 * Allows to track which {@code Thread} is running within which {@link TrackableContext Context}.
 * A {@code ContextTracker} instance is associated with one particular type of contexts represented
 * as a subclass of {@link TrackableContext}, here denoted as {@code CtxT}. For example: a
 * {@code Tracker} of which {@code Thread} runs within a {@link TrackableContext Context} of which
 * {@code HttpServletRequest}.
 * <p>
 * Instances should usually be created at app startup to be in turn used by instances of
 * {@link ContextScope}s.</p>
 * @see pl.morgwai.base.guice.scopes code organization guidelines for deriving libs in the package
 *     docs.
 */
public class ContextTracker<CtxT extends TrackableContext<CtxT>> {



	private final ThreadLocal<CtxT> currentContext = new ThreadLocal<>();



	/**
	 * Returns the current {@code Context} of type {@code CtxT}. If {@code CtxT} is not currently
	 * active for the calling {@code Thread}, then {@code null}.
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
	 * Retrieves from {@code trackers} all contexts active (current within their class) for the
	 * calling {@code  Thread}. The returned {@code List} can be then used as an argument to
	 * {@link TrackableContext#executeWithinAll(List, Runnable)} to transfer the contexts when
	 * switching to another {@code  Thread}.
	 * <p>
	 * Deriving libs should bind {@code List<ContextTracker<?>>} to an instance containing all
	 * possible trackers for use as an argument for this method.</p>
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

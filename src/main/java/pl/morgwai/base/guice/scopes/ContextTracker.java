// Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;



/**
 * Allows to track which {@code Thread} is running within which {@link TrackableContext Context}.
 * A {@code ContextTracker} instance is associated with one particular type of {@code Context}s
 * represented as a subclass of {@link TrackableContext}, here denoted as {@code ContextT}. For
 * example: a {@code Tracker} of which {@code Thread} runs within a {@link TrackableContext Context}
 * of which {@code HttpServletRequest}.
 * <p>
 * {@code ContextTracker} instances are usually created at an app startup to be in turn used by
 * instances of {@link ContextScope}s. See {@link ScopeModule} for details.</p>
 */
public class ContextTracker<ContextT extends TrackableContext<? super ContextT>> {



	private final ThreadLocal<ContextT> currentContext = new ThreadLocal<>();



	/**
	 * Returns the current {@code Context} of type {@code ContextT}.
	 * If {@code ContextT} is not currently active for the calling {@code Thread}, then
	 * {@code null}.
	 * @see #getActiveContexts(List)
	 */
	public ContextT getCurrentContext() {
		return currentContext.get();
	}



	/**
	 * Sets {@code ctx} as {@link #currentContext the current Context} for the calling
	 * {@code Thread} and executes {@code task} synchronously.
	 * Afterwards clears {@link #currentContext} for the {@code Thread}.
	 * <p>
	 * For internal use by{@link TrackableContext#executeWithinSelf(Throwing5Task)}.</p>
	 */
	<
		R,
		E1 extends Exception,
		E2 extends Exception,
		E3 extends Exception,
		E4 extends Exception,
		E5 extends Exception
	> R trackWhileExecuting(
		ContextT ctx,
		Throwing5Task<R, E1, E2, E3, E4, E5> task
	) throws E1, E2, E3, E4, E5 {
		currentContext.set(ctx);
		try {
			return task.execute();
		} finally {
			currentContext.remove();
		}
	}

	/**
	 * Variant of {@link #trackWhileExecuting(TrackableContext, Throwing5Task)} for
	 * {@link Runnable} tasks.
	 */
	void trackWhileExecuting(ContextT ctx, Runnable task) {
		currentContext.set(ctx);
		try {
			task.run();
		} finally {
			currentContext.remove();
		}
	}



	/**
	 * Retrieves from {@code trackers} all {@link TrackableContext}s active (current within their
	 * type) for the calling {@code  Thread}.
	 * The returned {@code List} can be then used as an argument to
	 * {@link TrackableContext#executeWithinAll(List, Throwing5Task)} to transfer the
	 * {@code Contexts} when switching to another {@code  Thread}. All
	 * {@link InducedContextScope Contexts induced} by any of the returned {@link TrackableContext}s
	 * will also "follow" automatically their inducers to the new {@code Thread}.
	 */
	public static List<TrackableContext<?>> getActiveContexts(List<ContextTracker<?>> trackers) {
		if (trackers.size() == 1) {  // optimize for the most common tracker count
			final var ctx = trackers.get(0).getCurrentContext();
			return ctx != null ? List.of(ctx) : List.of();
		}

		final var activeCtxs = new ArrayList<TrackableContext<?>>(trackers.size());
		for (var tracker: trackers) {
			final var ctx = tracker.getCurrentContext();
			if (ctx != null) activeCtxs.add(ctx);
		}
		return activeCtxs;
	}



	static final TypeLiteral<List<ContextTracker<?>>> ALL_TRACKERS_TYPE = new TypeLiteral<>() {};
	/**
	 * {@code Key} for the global {@code List} of all {@code ContextTracker}s provided by a given
	 * lib derived from {@code guice-context-scopes}.
	 */
	public static final Key<List<ContextTracker<?>>> ALL_TRACKERS_KEY = Key.get(ALL_TRACKERS_TYPE);
}

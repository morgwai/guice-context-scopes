// Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import static java.util.concurrent.Executors.callable;



/**
 * {@link InjectionContext} that can
 * {@link #executeWithinSelf(Callable) execute tasks within itself}, so that it can be tracked
 * across {@code Thread}s using its associated {@link ContextTracker}.
 * <p>
 * Subclasses must use themselves as {@code ContextT} type argument.</p>
 */
public abstract class TrackableContext<ContextT extends TrackableContext<ContextT>>
		extends InjectionContext {



	private transient ContextTracker<ContextT> tracker;



	protected TrackableContext(ContextTracker<ContextT> tracker) {
		this.tracker = tracker;
	}



	/**
	 * Asks the associated {@link ContextTracker} to set this {@code Context} as the current one for
	 * the calling {@code Thread} and executes {@code task} synchronously.
	 * Afterwards clears the current {@code Context} for the {@code Thread}.
	 * @see #executeWithinAll(List, Callable)
	 */
	public <T> T executeWithinSelf(Callable<T> task) throws Exception {
		@SuppressWarnings("unchecked")
		final var thisCtx = (ContextT) this;
		return tracker.trackWhileExecuting(thisCtx, task);
	}



	/** Variant of {@link #executeWithinSelf(Callable)} for {@link Runnable} {@code task}s. */
	public void executeWithinSelf(Runnable task) {
		try {
			executeWithinSelf(callable(task));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception neverHappens) {
			// result of wrapping with a Callable
		}
	}



	/**
	 * Executes {@code task} synchronously on the current {@code Thread} within all
	 * {@code Context}s.
	 * Used to transfer {@code Contexts} saved with {@link ContextTracker#getActiveContexts(List)}
	 * after dispatching to another {@code Thread}.
	 */
	public static <T> T executeWithinAll(List<TrackableContext<?>> contexts, Callable<T> task)
			throws Exception {
		switch (contexts.size()) {
			case 1:
				return contexts.get(0).executeWithinSelf(task);
			case 0:
				final var noCtxWarning = String.format(
					NO_CONTEXT_WARNING,
					Thread.currentThread().getName(),
					task.toString()
				);
				System.err.println(noCtxWarning);
				log.warning(noCtxWarning);
				return task.call();
			default:
				return executeWithinAll(
					contexts.subList(1, contexts.size()),
					new Callable<>() {
						@Override public T call() throws Exception {
							return contexts.get(0).executeWithinSelf(task);
						}
						@Override public String toString() {
							return task.toString();
						}
					}
				);
		}
	}

	static final String NO_CONTEXT_WARNING =
			"Thread \"%s\" is executing task %s outside of any Context";
	static final Logger log = Logger.getLogger(TrackableContext.class.getName());



	/** Variant of {@link #executeWithinAll(List, Callable)} for {@link Runnable} {@code task}s. */
	public static void executeWithinAll(List<TrackableContext<?>> contexts, Runnable task) {
		try {
			executeWithinAll(contexts, callable(task));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception neverHappens) {
			// result of wrapping with a Callable
		}
	}



	/**
	 * Sets a {@link ContextTracker} that will be used for tracking this {@code Context}.
	 * This method must be called once immediately after each deserialization of this
	 * {@code Context}.
	 * @throws IllegalStateException if a tracker for this {@code Context} has already been set.
	 */
	protected void setTracker(ContextTracker<ContextT> tracker) {
		if (this.tracker != null) throw new IllegalStateException("tracker already set");
		this.tracker = tracker;
	}



	private static final long serialVersionUID = 5613154173540357269L;
}

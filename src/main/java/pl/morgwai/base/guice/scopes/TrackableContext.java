// Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.logging.Logger;

import pl.morgwai.base.function.Throwing5Task;



/**
 * {@link InjectionContext} that can
 * {@link #executeWithinSelf(Throwing5Task) execute tasks within itself}, so that it can be
 * tracked across {@code Thread}s using its associated {@link ContextTracker}.
 * <p>
 * Subclasses must use themselves as {@code ContextT} type argument.</p>
 */
public abstract class TrackableContext<ContextT extends TrackableContext<ContextT>>
		extends InjectionContext {



	/** Associated {@link ContextTracker} that tracks this {@code Context} across {@code Thread}s.*/
	public ContextTracker<ContextT> getTracker() { return tracker; }
	private transient ContextTracker<ContextT> tracker;



	protected TrackableContext(ContextTracker<ContextT> tracker) {
		this.tracker = tracker;
	}



	/**
	 * Asks the associated {@link #getTracker() Tracker} to set this {@code Context} as the current
	 * one for the calling {@code Thread} and executes {@code task} synchronously.
	 * Afterwards clears the current {@code Context} for the {@code Thread}.
	 * @see #executeWithinAll(List, Throwing5Task)
	 */
	public <
		R,
		E1 extends Exception,
		E2 extends Exception,
		E3 extends Exception,
		E4 extends Exception,
		E5 extends Exception
	> R executeWithinSelf(Throwing5Task<R, E1, E2, E3, E4, E5> task) throws E1, E2, E3, E4, E5 {
		@SuppressWarnings("unchecked")
		final var thisCtx = (ContextT) this;
		return tracker.trackWhileExecuting(thisCtx, task);
	}

	/** Variant of {@link #executeWithinSelf(Throwing5Task)} for {@link Runnable} {@code task}s.*/
	public void executeWithinSelf(Runnable task) {
		@SuppressWarnings("unchecked")
		final var thisCtx = (ContextT) this;
		tracker.trackWhileExecuting(thisCtx, task);
	}



	/**
	 * Executes {@code task} synchronously on the current {@code Thread} within all
	 * {@code contexts}.
	 * Used to transfer {@code Contexts} saved with {@link ContextTracker#getActiveContexts(List)}
	 * after dispatching to another {@code Thread}.
	 */
	public static <
		R,
		E1 extends Exception,
		E2 extends Exception,
		E3 extends Exception,
		E4 extends Exception,
		E5 extends Exception
	> R executeWithinAll(
		List<TrackableContext<?>> contexts,
		Throwing5Task<R, E1, E2, E3, E4, E5> task
	) throws E1, E2, E3, E4, E5 {
		switch (contexts.size()) {
			case 1:
				return contexts.get(0).executeWithinSelf(task);
			case 0:
				printNoCtxWarning(task);
				return task.execute();
			default:
				return executeWithinAll(
					contexts.subList(1, contexts.size()),
					new Throwing5Task<R, E1, E2, E3, E4, E5>() {
						@Override public R execute() throws E1, E2, E3, E4, E5 {
							return contexts.get(0).executeWithinSelf(task);
						}
						@Override public String toString() {
							return task.toString();
						}
					}
				);
		}
	}

	/** Variant of {@link #executeWithinAll(List, Throwing5Task)} for {@link Runnable} tasks. */
	public static void executeWithinAll(List<TrackableContext<?>> contexts, Runnable task) {
		switch (contexts.size()) {
			case 1:
				contexts.get(0).executeWithinSelf(task);
				return;
			case 0:
				printNoCtxWarning(task);
				task.run();
				return;
			default:
				executeWithinAll(
					contexts.subList(1, contexts.size()),
					new Runnable() {
						@Override public void run() {
							contexts.get(0).executeWithinSelf(task);
						}
						@Override public String toString() {
							return task.toString();
						}
					}
				);
		}
	}

	static void printNoCtxWarning(Object task) {
		final var noCtxWarning = String.format(
			NO_CONTEXT_WARNING,
			Thread.currentThread().getName(),
			task.toString()
		);
		System.err.println(noCtxWarning);
		log.warning(noCtxWarning);
	}

	static final String NO_CONTEXT_WARNING =
			"Thread \"%s\" is executing task %s outside of any Context";
	static final Logger log = Logger.getLogger(TrackableContext.class.getName());



	/**
	 * Sets a {@link #getTracker() Tracker} that will be used for tracking this {@code Context}.
	 * This method must be called once immediately after each deserialization of this
	 * {@code Context}.
	 * @throws IllegalStateException if a tracker for this {@code Context} has already been set.
	 */
	protected void setTracker(ContextTracker<ContextT> tracker) {
		if (this.tracker != null) throw new IllegalStateException("tracker already set");
		this.tracker = tracker;
	}



	private static final long serialVersionUID = -7387829122326042943L;
}

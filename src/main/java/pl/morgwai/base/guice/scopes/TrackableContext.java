// Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.logging.Logger;

import pl.morgwai.base.function.*;



/**
 * {@link InjectionContext} that can
 * {@link #executeWithinSelf(Throwing4Computation) execute tasks within itself}, so that it can be
 * tracked across {@code Thread}s using its associated {@link ContextTracker}.
 * <p>
 * Subclasses must use themselves as {@code ContextT} type argument.</p>
 */
public abstract class TrackableContext<ContextT extends TrackableContext<ContextT>>
		extends InjectionContext {



	/** Associated {@link ContextTracker} that tracks this {@code Context} across {@code Thread}s.*/
	public ContextTracker<ContextT> getTracker() { return tracker; }
	private transient ContextTracker<ContextT> tracker;



	/** See {@link InjectionContext#InjectionContext(InjectionContext) super}. */
	protected TrackableContext(InjectionContext parentCtx, ContextTracker<ContextT> tracker) {
		super(parentCtx);
		this.tracker = tracker;
	}

	protected TrackableContext(ContextTracker<ContextT> tracker) {
		this(null, tracker);
	}



	/**
	 * Asks the associated {@link #getTracker() Tracker} to set this {@code Context} as the current
	 * one for the calling {@code Thread} and executes {@code task} synchronously.
	 * Afterwards clears the current {@code Context} for the {@code Thread}.
	 * @see #executeWithinAll(List, Throwing4Computation)
	 */
	public <
		R, E1 extends Throwable, E2 extends Throwable, E3 extends Throwable, E4 extends Throwable
	> R executeWithinSelf(Throwing4Computation<R, E1, E2, E3, E4> task) throws E1, E2, E3, E4 {
		@SuppressWarnings("unchecked")
		final var thisCtx = (ContextT) this;
		return tracker.trackWhileExecuting(thisCtx, task);
	}

	/** Variant of {@link #executeWithinSelf(Throwing4Computation)} for {@link ThrowingTask}s. */
	public <
		E1 extends Throwable, E2 extends Throwable, E3 extends Throwable, E4 extends Throwable
	> void executeWithinSelf(Throwing4Task<E1, E2, E3, E4> task) throws E1, E2, E3, E4 {
		executeWithinSelf(ThrowingComputation.of(task));
	}

	/** Variant of {@link #executeWithinSelf(Throwing4Computation)} for {@link Runnable}s. */
	public void executeWithinSelf(Runnable task) {
		// implemented directly to avoid additional wrapping of tiny tasks passed between Executors
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
		R, E1 extends Throwable, E2 extends Throwable, E3 extends Throwable, E4 extends Throwable
	> R executeWithinAll(
		List<TrackableContext<?>> contexts,
		Throwing4Computation<R, E1, E2, E3, E4> task
	) throws E1, E2, E3, E4 {
		switch (contexts.size()) {
			case 1:
				return contexts.get(0).executeWithinSelf(task);
			case 0:
				printNoCtxWarning(task);
				return task.perform();
			default:
				return executeWithinAll(
					contexts.subList(1, contexts.size()),
					(Throwing4Computation<R, E1, E2, E3, E4>)
							() -> contexts.get(0).executeWithinSelf(task)
				);
		}
	}

	/**
	 * Variant of {@link #executeWithinAll(List, Throwing4Computation)} for {@link ThrowingTask}s.
	 */
	public static <
		E1 extends Throwable, E2 extends Throwable, E3 extends Throwable, E4 extends Throwable
	> void executeWithinAll(
		List<TrackableContext<?>> contexts,
		Throwing4Task<E1, E2, E3, E4> task
	) throws E1, E2, E3, E4 {
		executeWithinAll(contexts, ThrowingComputation.of(task));
	}

	/** Variant of {@link #executeWithinAll(List, Throwing4Computation)} for {@link Runnable}s. */
	public static void executeWithinAll(List<TrackableContext<?>> contexts, Runnable task) {
		// implemented directly to avoid additional wrapping of tiny tasks passed between Executors
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
					(Runnable) () -> contexts.get(0).executeWithinSelf(task)
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



	private static final long serialVersionUID = 1563722492904416901L;
}

// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;



/**
 * An {@link InjectionContext} that can {@link #executeWithinSelf(Runnable) execute tasks within
 * itself}, so that it can be tracked across {@code Threads} using its associated
 * {@link ContextTracker}.
 * <p>
 * Subclasses must use themselves as {@code CtxT} type argument.</p>
 */
public abstract class TrackableContext<CtxT extends TrackableContext<CtxT>>
		extends InjectionContext {



	final ContextTracker<CtxT> tracker;



	protected TrackableContext(ContextTracker<CtxT> tracker) {
		this.tracker = tracker;
	}



	/**
	 * Asks the associated {@link ContextTracker} to set this {@code Context} as the current one for
	 * the calling {@code  Thread} and executes {@code task} synchronously. Afterwards clears the
	 * current context.
	 * @see #executeWithinAll(List, Callable)
	 */
	public <T> T executeWithinSelf(Callable<T> task) throws Exception {
		@SuppressWarnings("unchecked")
		final var thisCtx = (CtxT) this;
		return tracker.trackWhileExecuting(thisCtx, task);
	}



	/** Version of {@link #executeWithinSelf(Callable)} that takes a {@link Runnable} param. */
	public void executeWithinSelf(Runnable task) {
		try {
			executeWithinSelf(new CallableRunnable(task));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception neverHappens) {
			// result of wrapping with a Callable
		}
	}



	/**
	 * Executes {@code task} synchronously on the current thread within all {@code contexts}.
	 * Used to transfer contexts saved with {@link ContextTracker#getActiveContexts(List)} after a
	 * switch to another thread.
	 */
	public static <T> T executeWithinAll(List<TrackableContext<?>> contexts, Callable<T> task)
			throws Exception {
		switch (contexts.size()) {
			case 1:
				return contexts.get(0).executeWithinSelf(task);
			case 0:
				final var warningMessage = "thread \"" + Thread.currentThread().getName()
						+ "\" is executing task " + task + " outside of any context";
				System.err.println(warningMessage);
				log.warning(warningMessage);
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

	static final Logger log = Logger.getLogger(TrackableContext.class.getName());



	/** Version of {@link #executeWithinAll(List, Callable)} that takes a {@link Runnable} param. */
	public static void executeWithinAll(List<TrackableContext<?>> contexts, Runnable task) {
		try {
			executeWithinAll(contexts, new CallableRunnable(task));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception neverHappens) {
			// result of wrapping with a Callable
		}
	}



	static class CallableRunnable implements Callable<Void> {

		final Runnable wrappedTask;

		protected CallableRunnable(Runnable taskToWrap) {
			this.wrappedTask = taskToWrap;
		}

		@Override
		public Void call() {
			wrappedTask.run();
			return null;
		}

		@Override
		public String toString() {
			return wrappedTask.toString();
		}
	}



	private static final long serialVersionUID = -1676033543790587340L;
}

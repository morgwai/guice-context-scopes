// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;



/**
 * An {@link InjectionContext} that can {@link #executeWithinSelf(Runnable) execute operations
 * within itself}, so that it can be tracked across threads using the associated
 * {@link ContextTracker}.
 * <p>
 * Overriding classes must use themselves as {@code CtxT} type argument.</p>
 */
public abstract class TrackableContext<CtxT extends TrackableContext<CtxT>>
		extends InjectionContext {



	final ContextTracker<CtxT> tracker;



	protected TrackableContext(ContextTracker<CtxT> tracker) {
		this.tracker = tracker;
	}



	/**
	 * Sets itself as the current context for the current thread and executes {@code task}
	 * synchronously. Afterwards clears the current context.
	 *
	 * @see TrackableContext#executeWithinAll(List, Runnable)
	 */
	public void executeWithinSelf(Runnable task) {
		try {
			executeWithinSelf(new CallableRunnable(task));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception ignored) {}  // dead code: result of wrapping task with a Callable
	}



	/**
	 * Sets itself as the current context for the current thread and executes {@code task}
	 * synchronously. Afterwards clears the current context.
	 *
	 * @see TrackableContext#executeWithinAll(List, Callable)
	 */
	public <T> T executeWithinSelf(Callable<T> task) throws Exception {
		@SuppressWarnings("unchecked")
		final var thisCtx = (CtxT) this;
		return tracker.trackWhileExecuting(thisCtx, task);
	}



	/**
	 * Executes {@code task} on the current thread within all {@code contexts}.
	 * Used to transfer active contexts after a switch to another thread.
	 * @see ContextTracker#getActiveContexts(List)
	 */
	public static void executeWithinAll(List<TrackableContext<?>> contexts, Runnable task) {
		try {
			executeWithinAll(contexts, new CallableRunnable(task));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception ignored) {}  // dead code: result of wrapping task with a Callable
	}



	/**
	 * Executes {@code task} on the current thread within all {@code contexts}.
	 * Used to transfer active contexts after a switch to another thread.
	 * @see ContextTracker#getActiveContexts(List)
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
}



class CallableRunnable implements Callable<Void> {

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

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
			tracker.trackWhileExecuting(thisCtx, new CallableRunnable(task));
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



abstract class Wrapper {

	final Object wrapped;

	protected Wrapper(Object toWrap) { this.wrapped = toWrap; }

	public Object unwrap() {
		Object unwrapped = wrapped;
		while (unwrapped instanceof Wrapper) unwrapped = ((Wrapper) unwrapped).wrapped;
		return unwrapped;
	}

	@Override public String toString() { return wrapped.toString(); }
}



abstract class RunnableWrapper extends Wrapper implements Runnable {
	protected RunnableWrapper(Object toWrap) { super(toWrap); }
}



abstract class CallableWrapper<T> extends Wrapper implements Callable<T> {
	protected CallableWrapper(Object toWrap) { super(toWrap); }
}



class CallableRunnable extends CallableWrapper<Void> {

	CallableRunnable(Runnable toWrap) { super(toWrap); }

	@Override
	public Void call() {
		((Runnable) wrapped).run();
		return null;
	}
}

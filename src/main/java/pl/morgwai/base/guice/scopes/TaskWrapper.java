// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.Callable;



/**
 * A multi-layer wrapper around a {@link Callable} or a {@link Runnable} that allows to obtain the
 * original wrapped task.
 */
abstract class TaskWrapper {



	final Object wrappedTask;



	protected TaskWrapper(Runnable taskToWrap) {
		this.wrappedTask = taskToWrap;
	}



	protected TaskWrapper(Callable<?> taskToWrap) {
		this.wrappedTask = taskToWrap;
	}



	Object unwrap() {
		var unwrapped = wrappedTask;
		while (unwrapped instanceof TaskWrapper) unwrapped = ((TaskWrapper) unwrapped).wrappedTask;
		return unwrapped;
	}



	@Override
	public String toString() {
		return wrappedTask.toString();
	}
}



/**
 * For inlining anonymous {@link Runnable}s wrapping other tasks.
 * See {@link ContextTrackingExecutor#execute(Callable)}.
 */
abstract class RunnableWrapper extends TaskWrapper implements Runnable {
	protected RunnableWrapper(Runnable taskToWrap) { super(taskToWrap); }
	protected RunnableWrapper(Callable<?> taskToWrap) { super(taskToWrap); }
}



/**
 * For inlining anonymous {@link Callable}s wrapping other tasks.
 * See {@link ContextTrackingExecutor#executeWithinAll(java.util.List, Callable)}.
 */
abstract class CallableWrapper<T> extends TaskWrapper implements Callable<T> {
	protected CallableWrapper(Runnable taskToWrap) { super(taskToWrap); }
	protected CallableWrapper(Callable<T> taskToWrap) { super(taskToWrap); }
}



/**
 * For converting a {@link Runnable} into a {@link Callable}.
 * See {@link TrackableContext#executeWithinSelf(Runnable)}.
 */
class CallableRunnable extends CallableWrapper<Void> {



	CallableRunnable(Runnable taskToWrap) {
		super(taskToWrap);
	}



	@Override
	public Void call() {
		((Runnable) wrappedTask).run();
		return null;
	}
}

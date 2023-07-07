// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.Callable;



/**
 * A multi-layer wrapper around a {@link Callable} or a {@link Runnable} that allows to obtain the
 * original task.
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



abstract class RunnableWrapper extends TaskWrapper implements Runnable {
	// unused: protected RunnableWrapper(Runnable taskToWrap) { super(taskToWrap); }
	protected RunnableWrapper(Callable<?> taskToWrap) { super(taskToWrap); }
}



abstract class CallableWrapper<T> extends TaskWrapper implements Callable<T> {
	protected CallableWrapper(Runnable taskToWrap) { super(taskToWrap); }
	protected CallableWrapper(Callable<T> taskToWrap) { super(taskToWrap); }
}



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

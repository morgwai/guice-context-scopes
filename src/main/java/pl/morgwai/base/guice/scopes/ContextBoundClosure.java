// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;



/**
 * Base class for decorators that execute their wrapped closures within supplied
 * {@link TrackableContext Contexts}.
 */
public abstract class ContextBoundClosure<ClosureT> {



	public List<TrackableContext<?>> getContexts() { return contexts; }
	public final List<TrackableContext<?>> contexts;

	public ClosureT getBoundClosure() { return boundClosure; }
	public final ClosureT boundClosure;



	protected ContextBoundClosure(List<TrackableContext<?>> contexts, ClosureT closureToBind) {
		this.contexts = List.copyOf(contexts);
		this.boundClosure = closureToBind;
	}



	@Override
	public String toString() {
		return "ContextBoundClosure { closure = " + boundClosure + " }";
	}



	/** For passing to {@link TrackableContext#executeWithinAll(List, Runnable)} in subclasses. */
	protected class RunnableWrapper implements Runnable {

		final Runnable runnableWrappedClosure;

		public RunnableWrapper(Runnable runnableWrappedClosure) {
			this.runnableWrappedClosure = runnableWrappedClosure;
		}

		@Override public void run() {
			runnableWrappedClosure.run();
		}

		@Override public String toString() {
			return boundClosure.toString();
		}
	}



	/** For passing to {@link TrackableContext#executeWithinAll(List, Callable)} in subclasses. */
	protected class CallableWrapper<T> implements Callable<T> {

		final Callable<T> callableWrappedClosure;

		protected CallableWrapper(Callable<T> callableWrappedClosure) {
			this.callableWrappedClosure = callableWrappedClosure;
		}

		@Override public T call() throws Exception {
			return callableWrappedClosure.call();
		}

		@Override public String toString() {
			return boundClosure.toString();
		}
	}
}

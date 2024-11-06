// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;



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



	/**
	 * Provides nice {@link Object#toString() toString()} for wrapped lambdas passed to
	 * {@link TrackableContext#executeWithinAll(List, Runnable)} in subclasses.
	 */
	protected class RunnableWrapper implements Runnable {

		final Runnable wrappedTask;

		public RunnableWrapper(Runnable taskToWrap) {
			this.wrappedTask = taskToWrap;
		}

		@Override public void run() {
			wrappedTask.run();
		}

		@Override public String toString() {
			return boundClosure.toString();
		}
	}



	/**
	 * Provides nice {@link Object#toString() toString()} for wrapped lambdas passed to
	 * {@link TrackableContext#executeWithinAll(List, ThrowingTask)} in subclasses.
	 */
	protected class ThrowingTaskWrapper<
		R,
		E1 extends Exception,
		E2 extends Exception,
		E3 extends Exception
	> implements ThrowingTask<R, E1, E2, E3> {

		final ThrowingTask<R, E1, E2, E3> wrappedTask;

		public ThrowingTaskWrapper(ThrowingTask<R, E1, E2, E3> taskToWrap) {
			this.wrappedTask = taskToWrap;
		}

		@Override public R execute() throws E1, E2, E3 {
			return wrappedTask.execute();
		}

		@Override public String toString() {
			return boundClosure.toString();
		}
	}
}

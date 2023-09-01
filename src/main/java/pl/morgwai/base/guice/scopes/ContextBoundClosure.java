// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;



/** Base class for decorators that will execute their wrapped closures within supplied contexts. */
public abstract class ContextBoundClosure<ClosureT> {



	public List<TrackableContext<?>> getContexts() { return contexts; }
	protected final List<TrackableContext<?>> contexts;

	public ClosureT getBoundClosure() { return boundClosure; }
	protected final ClosureT boundClosure;



	protected ContextBoundClosure(List<TrackableContext<?>> contexts, ClosureT closureToBind) {
		this.contexts = List.copyOf(contexts);
		this.boundClosure = closureToBind;
	}



	@Override
	public String toString() {
		return "ContextBoundClosure { closure = " + boundClosure + " }";
	}



	/** For passing to {@link TrackableContext#executeWithinAll(List, Runnable)} in subclasses. */
	protected abstract class RunnableWrapper implements Runnable {

		@Override public String toString() {
			return boundClosure.toString();
		}
	}



	/** For passing to {@link TrackableContext#executeWithinAll(List, Callable)} in subclasses. */
	protected abstract class CallableWrapper<T> implements Callable<T> {

		@Override public String toString() {
			return boundClosure.toString();
		}
	}
}

// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;



/** Base class for decorators that will execute wrapped closure within supplied contexts. */
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
}

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



	static String quote(Object o) {
		return o instanceof String ? "\"" : o instanceof Character ? "'" : "";
	}
}

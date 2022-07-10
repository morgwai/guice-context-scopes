// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.function.Function;



/**
 * Scopes object to a context induced by another context obtained from the associated
 * {@link ContextTracker}. For example HTTP session context may be induced by HTTP servlet request
 * context.
 */
public class InducedContextScope<
		InducingCtxT extends TrackableContext<InducingCtxT>,
		InducedCtxT extends InjectionContext
	> extends ContextScope<InducingCtxT> {



	final Function<InducingCtxT, InducedCtxT> inducer;



	public InducedContextScope(
			String name,
			ContextTracker<InducingCtxT> tracker,
			Function<InducingCtxT, InducedCtxT> inducer) {
		super(name, tracker);
		this.inducer = inducer;
	}



	protected InjectionContext getContext() {
		return inducer.apply(tracker.getCurrentContext());
	}
}

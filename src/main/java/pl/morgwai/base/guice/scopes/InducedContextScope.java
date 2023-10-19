// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.function.Function;



/**
 * Scopes objects to the context induced by the context obtained from the associated
 * {@link ContextTracker}. For example HTTP session context may be induced by HTTP servlet request
 * context.
 */
public class InducedContextScope<
			BaseCtxT extends TrackableContext<BaseCtxT>,
			InducedCtxT extends InjectionContext
		> extends ContextScope<BaseCtxT> {



	final Function<BaseCtxT, InducedCtxT> inducer;



	public InducedContextScope(
		String name,
		ContextTracker<BaseCtxT> tracker,
		Function<BaseCtxT, InducedCtxT> inducer
	) {
		super(name, tracker);
		this.inducer = inducer;
	}



	protected InjectionContext getContext() {
		return inducer.apply(tracker.getCurrentContext());
	}
}

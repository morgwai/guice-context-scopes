// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.function.Function;



/**
 * Scopes objects to the context induced by the context obtained from the associated
 * {@link ContextTracker}. For example HTTP session context may be induced by HTTP servlet request
 * context.
 * <p>
 * Note: induced Scopes may also be implemented by subclassing {@link ContextScope} and overriding
 * {@link ContextScope#getContext()} method to return the induced context instead of the one
 * directly obtained from {@link #tracker}. Both these ways are mostly equivalent.</p>
 */
public class InducedContextScope<
		TrackableCtxT extends TrackableContext<TrackableCtxT>,
		InducedCtxT extends InjectionContext
	> extends ContextScope<TrackableCtxT> {



	final Function<TrackableCtxT, InducedCtxT> inducer;



	public InducedContextScope(
		String name,
		ContextTracker<TrackableCtxT> tracker,
		Function<TrackableCtxT, InducedCtxT> inducer
	) {
		super(name, tracker);
		this.inducer = inducer;
	}



	protected InjectionContext getContext() {
		return inducer.apply(tracker.getCurrentContext());
	}
}

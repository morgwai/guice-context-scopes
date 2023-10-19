// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.function.Function;



/**
 * Scopes objects to the instance of {@code InducedCtxT} that is <i>induced</i> by the
 * {@code BaseCtxT} instance obtained from the associated {@link ContextTracker} at a given moment.
 * For example, entering the context of an HTTP request induces entering the context of the HTTP
 * session, which this request belongs to.
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

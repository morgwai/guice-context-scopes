// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.function.Function;



/**
 * Scopes objects to the instance of {@code InducedContextT} that is <i>induced</i> by the
 * {@code BaseContextT} instance obtained from the associated {@link ContextTracker} at a given
 * moment. For example, entering the context of an HTTP request induces entering the context of the
 * HTTP session, which this request belongs to.
 */
public class InducedContextScope<
			BaseContextT extends TrackableContext<BaseContextT>,
			InducedContextT extends InjectionContext
		> extends ContextScope<BaseContextT> {



	final Function<BaseContextT, InducedContextT> inducer;



	public InducedContextScope(
		String name,
		ContextTracker<BaseContextT> tracker,
		Function<BaseContextT, InducedContextT> inducer
	) {
		super(name, tracker);
		this.inducer = inducer;
	}



	protected InjectionContext getContext() {
		return inducer.apply(tracker.getCurrentContext());
	}
}

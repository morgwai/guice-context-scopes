// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.function.Function;



/**
 * Scopes objects to the instance of {@code InducedContextT} that is <i>induced</i> by the current
 * at a given moment {@code BaseContextT} instance (obtained from the associated
 * {@link ContextTracker}).
 * For example, entering the context of an HTTP request induces entering the context of the HTTP
 * session, which this request belongs to.
 */
public class InducedContextScope<
			BaseContextT extends TrackableContext<? super BaseContextT>,
			InducedContextT extends InjectionContext
		> extends ContextScope<BaseContextT> {



	final Function<? super BaseContextT, ? extends InducedContextT> inducedCtxRetriever;



	/**
	 * Constructs a new instance.
	 * @param inducedCtxRetriever retrieves the instance of {@code InducedContextT} that is induced
	 *     by a given {@code BaseContextT} instance argument. For example an
	 *     {@code inducedCtxRetriever} for {@code HttpSessionScope} should return the
	 *     {@code Context} of the HTTP session to which the HTTP request given by an argument
	 *     {@code HttpRequestContext} belongs to.
	 */
	public InducedContextScope(
		String name,
		ContextTracker<BaseContextT> tracker,
		Function<? super BaseContextT, ? extends InducedContextT> inducedCtxRetriever
	) {
		super(name, tracker);
		this.inducedCtxRetriever = inducedCtxRetriever;
	}



	/**
	 * Returns the {@code InducedContextT} instance induced by a {@code BaseContextT} instance
	 * obtained from {@link #tracker}.
	 */
	protected InducedContextT getContext() {
		return inducedCtxRetriever.apply(tracker.getCurrentContext());
	}
}

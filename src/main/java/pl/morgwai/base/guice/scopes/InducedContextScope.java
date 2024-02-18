// Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.function.Function;



/**
 * Scopes objects to the instance of {@code InducedContextT} that is <i>induced</i> by the current
 * {@code BaseContextT} instance from the associated {@link ContextTracker}.
 * For example, entering the {@code Context} of an {@code HttpServletRequest} induces entering the
 * {@code Context} of the {@code HttpSession}, to which this {@code HttpServletRequest} belongs.
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
	 *     {@code Context} of the {@code HttpSession}, to which a given {@code HttpServletRequest}
	 *     belongs.
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
	 * Applies {@link #InducedContextScope(String, ContextTracker, Function) the retriever Function}
	 * to the {@code BaseContextT} obtained from {@link #tracker}.
	 * @return the {@code InducedContextT} instance induced by the {@code BaseContextT} current
	 *     during a given {@link ScopedProvider#get() provisioning}.
	 */
	protected InducedContextT getContext() {
		return inducedCtxRetriever.apply(tracker.getCurrentContext());
	}
}

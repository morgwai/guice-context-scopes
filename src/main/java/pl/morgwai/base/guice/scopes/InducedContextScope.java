// Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.function.Function;



/**
 * Scopes objects to the {@code InducedContextT} instance <i>induced</i> by the instance of
 * {@code BaseContextT} current during a given
 * {@link com.google.inject.Provider#get() provisioning}.
 * For example, entering the {@code Context} of an {@code HttpServletRequest} induces entering the
 * {@code Context} of the {@code HttpSession}, to which this {@code HttpServletRequest} belongs.
 * <p>
 * Current {@code BaseContextT} instances are obtained directly from the associated
 * {@link #tracker}.</p>
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
	 *     {@code inducedCtxRetriever} for {@code httpSessionScope} should return the
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
	 * Applies {@code inducedCtxRetriever} {@link Function} (passed via
	 * {@link #InducedContextScope(String, ContextTracker, Function) the constructor}) to a
	 * {@code BaseContextT} obtained from {@link #tracker}.
	 * @return the current {@code InducedContextT} (induced by the current {@code BaseContextT}).
	 */
	@Override
	protected InducedContextT getContext() {
		return inducedCtxRetriever.apply(tracker.getCurrentContext());
	}
}

// Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.*;



/**
 * Scopes objects to the {@link InjectionContext Context} that is current during a given
 * {@link Provider#get() provisioning}.
 * <p>
 * A {@code  ContextScope} instance is associated with one particular subclass of
 * {@link InjectionContext}: for example an instance called {@code httpRequestScope} could be
 * associated with a class called {@code HttpRequestContext}, that stores objects scoped to
 * processing of HTTP requests.<br/>
 * By default a {@code ContextScope} instance is associated with {@code ContextT} class and its
 * instances are {@link #getContext() obtained} directly from the associated
 * {@link ContextTracker} passed via {@link #ContextScope(String, ContextTracker) the contructor}.
 * </p>
 * <p>
 * {@code  ContextScope} instances are usually created at an app startup to be used in bindings in
 * user {@link com.google.inject.Module}s. See code organization guidelines for deriving libs in
 * {@link pl.morgwai.base.guice.scopes the package docs}.</p>
 */
public class ContextScope<ContextT extends TrackableContext<? super ContextT>> implements Scope {



	protected final ContextTracker<ContextT> tracker;

	/** Name of this {@code Scope} for logging and debugging purposes. */
	public final String name;
	public String getName() { return name; }



	public ContextScope(String name, ContextTracker<ContextT> tracker) {
		this.name = name;
		this.tracker = tracker;
	}



	/** Wraps {@code producer} with a {@link ScopedProvider}. */
	@Override
	public <T> ScopedProvider<T> scope(Key<T> key, Provider<T> producer) {
		return new ScopedProvider<>(key, producer);
	}



	/**
	 * Returned by {@link #scope(Key, Provider)}, provides objects scoped to the
	 * {@link #getContext() Context current} at the moment of a given {@link #get() provisioning}.
	 */
	public class ScopedProvider<T> implements Provider<T> {

		final Key<T> key;
		final Provider<T> producer;



		ScopedProvider(Key<T> key, Provider<T> producer) {
			this.key = key;
			this.producer = producer;
		}



		/**
		 * Provides an object scoped to the {@code Context} returned by a call to
		 * {@link #getContext()}.
		 * @throws OutOfScopeException if the current {@code Thread} is running outside of any
		 *     {@code Context} of the associated {@code ContextT} type. This most commonly happens
		 *     if an async task was not
		 *     {@link ContextBinder#bindToContext(Runnable) bound to its current Context} before
		 *     being dispatched to this {@code Thread}. Use {@code ContextTrackingExecutor}s that
		 *     transfer {@code Context}s automatically or bind manually using {@link ContextBinder}.
		 */
		@Override public T get() {
			try {
				return getContext().produceIfAbsent(key, producer);
			} catch (NullPointerException e) {
				throw new OutOfScopeException(
						String.format(NO_CONTEXT_MESSAGE, name, Thread.currentThread().getName()));
			}
		}

		static final String NO_CONTEXT_MESSAGE = "no Context of Scope \"%s\" in Thread \"%s\": "
				+ "see the javadoc for ContextScope.ScopedProvider.get() -> "
				+ "https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest/pl/morgwai/"
				+ "base/guice/scopes/ContextScope.ScopedProvider.html#get()";



		@Override public String toString() {
			return "ScopedProvider { scope = \"" + name + "\", key = " + key + ", producer = "
					+ producer + " }";
		}
	}



	/**
	 * Returns the {@code Context} from which a scoped object should be obtained.
	 * Called during each {@link ScopedProvider#get() provisioning}. By default returns a
	 * {@code Context} {@link ContextTracker#getCurrentContext() obtained} directly from
	 * {@link #tracker}. May be overridden for example to return some {@code Context} induced by the
	 * one from {@link #tracker} (see {@link InducedContextScope}).
	 */
	protected InjectionContext getContext() {
		return tracker.getCurrentContext();
	}



	/** Returns {@link #name}. */
	@Override
	public String toString() {
		return name;
	}
}

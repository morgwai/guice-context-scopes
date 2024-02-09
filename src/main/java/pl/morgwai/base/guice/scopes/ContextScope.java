// Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.*;



/**
 * Scopes objects to the {@code  ContextT} instance that is current at a given moment (obtained from
 * the associated {@link ContextTracker} by default).
 * A {@code  ContextScope} instance is associated with one particular type of contexts: a subclass
 * of {@link TrackableContext}, here denoted as {@code ContextT}. For example: a {@code Scope} of
 * {@code Contexts} of {@code HttpServletRequests}.
 * <p>
 * Instances should usually be created at app startup to be used in bindings in user
 * {@link com.google.inject.Module}s.</p>
 * @see pl.morgwai.base.guice.scopes code organization guidelines for deriving libs in the package
 *     docs.
 */
public class ContextScope<ContextT extends TrackableContext<? super ContextT>> implements Scope {



	protected final ContextTracker<ContextT> tracker;

	public String getName() { return name; }
	final String name;



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
	 * Returned by {@link #scope(Key, Provider)}, provides objects scoped to a {@code Context} that
	 * is current at the moment {@link #get()} is called (as returned by {@link #getContext()}).
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
		 *     {@code Context}. This most commonly happens if an async task was not
		 *     {@link ContextBinder#bindToContext(Runnable) bound to the Context} before being
		 *     dispatched to the current {@code Thread}. Use {@code ContextTrackingExecutor}s that
		 *     transfer {@code Contexts} automatically or bind manually using {@link ContextBinder}.
		 */
		@Override public T get() {
			try {
				return getContext().produceIfAbsent(key, producer);
			} catch (NullPointerException e) {
				throw new OutOfScopeException("no Context of Scope \"" + name + "\" in Thread \""
						+ Thread.currentThread().getName() + "\". See the javadoc for "
						+ "ContextScope.ScopedProvider.get() -> https://javadoc.io/doc/pl.morgwai."
						+ "base/guice-context-scopes/latest/pl/morgwai/base/guice/scopes/"
						+ "ContextScope.ScopedProvider.html#get()");
			}
		}



		@Override public String toString() {
			return "ScopedProvider { scope = \"" + name + "\", key = " + key + ", producer = "
					+ producer + " }";
		}
	}



	/**
	 * Returns the {@code Context} from which scoped objects should be obtained.
	 * Called during each {@link ScopedProvider#get() provisioning}. By default returns the current
	 * {@code Context} obtained directly from {@link #tracker}. May be overridden for example to
	 * return some {@code Context} induced by the one from the {@link #tracker}.
	 * @see InducedContextScope
	 */
	protected InjectionContext getContext() {
		return tracker.getCurrentContext();
	}



	@Override
	public String toString() {
		return "ContextScope { name = \"" + name + "\" }";
	}
}

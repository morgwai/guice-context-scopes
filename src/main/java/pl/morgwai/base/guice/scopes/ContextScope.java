// Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.*;



/**
 * Scopes {@code Object}s to the {@link InjectionContext Context} that is current during a given
 * {@link Provider#get() provisioning}.
 * <p>
 * A {@code  ContextScope} instance is associated with one particular subclass of
 * {@link InjectionContext}: for example an instance called {@code httpRequestScope} could be
 * associated with a class called {@code HttpRequestContext} that stores {@code Object}s scoped to
 * processing of HTTP requests.<br/>
 * By default a {@code ContextScope} instance is associated with {@code ContextT} class and its
 * instances are {@link #getCurrentContext()  obtained} directly from the associated
 * {@link #tracker ContextTracker}.</p>
 * <p>
 * {@code  ContextScope} instances are usually created at the startup of an app to be used in
 * bindings in user {@link com.google.inject.Module}s.</p>
 * @see ScopeModule
 */
public class ContextScope<ContextT extends TrackableContext<? super ContextT>> implements Scope {



	/** The associated {@link ContextTracker} for obtaining {@code ContextT} instances. */
	public final ContextTracker<ContextT> tracker;
	public ContextTracker<ContextT> getTracker() { return tracker; }

	/** Name of this {@code Scope} for logging and debugging purposes. */
	public final String name;
	public String getName() { return name; }



	public ContextScope(String name, ContextTracker<ContextT> tracker) {
		this.name = name;
		this.tracker = tracker;
	}



	/**
	 * Scopes {@code Object}s to the {@code Context} returned by {@link #getCurrentContext()} at the
	 * moment of a given {@link Provider#get() provisioning}.
	 * <p>
	 * The returned scoped {@link Provider} will throw an {@link OutOfScopeException} if its called
	 * in a {@code Thread} running outside of any {@code Context} of {@code ContextT} type
	 * associated with this {@link ContextScope}.<br/>
	 * This most commonly happens if some async task was not
	 * {@link ContextBinder#bindToContext(Runnable) bound to its current Context} before being
	 * dispatched to that {@code Thread}. Use a {@link ContextTrackingExecutor} wrapper to transfer
	 * {@code Context}s automatically when passing tasks to {@link java.util.concurrent.Executor}s
	 * or bind manually using a {@link ContextBinder}.</p>
	 */
	@Override
	public <T> Provider<T> scope(Key<T> key, Provider<T> producer) {
		return new Provider<>() {
			@Override public T get() {
				try {
					return getCurrentContext().produceIfAbsent(key, producer);
				} catch (NullPointerException e) {
					throw new OutOfScopeException(String.format(
							NO_CONTEXT_MESSAGE, name, Thread.currentThread().getName()));
				}
			}
			@Override public String toString() {
				return "ScopedProvider { scope = \"" + name + "\", key = " + key + ", producer = "
						+ producer + " }";
			}
		};
	}

	static final String NO_CONTEXT_MESSAGE = "no Context of Scope \"%s\" in Thread \"%s\": "
			+ "see the javadoc for ContextScope.scope(...) -> "
			+ "https://javadoc.io/doc/pl.morgwai.base/guice-context-scopes/latest"
			+ "/pl/morgwai/base/guice/scopes/ContextScope.html"
			+ "#scope(com.google.inject.Key,com.google.inject.Provider)";



	/**
	 * Returns the {@link InjectionContext Context} from which a scoped {@code Object} should be
	 * obtained.
	 * Called during each {@link Provider#get() provisioning}. By default returns a {@code ContextT}
	 * {@link ContextTracker#getCurrentContext() obtained} directly from {@link #tracker}. May be
	 * overridden for example to return some {@code Context} induced by {@code ContextT} (see
	 * {@link InducedContextScope}).
	 */
	protected InjectionContext getCurrentContext() {
		return tracker.getCurrentContext();
	}



	/** Returns {@link #name}. */
	@Override
	public String toString() {
		return name;
	}
}

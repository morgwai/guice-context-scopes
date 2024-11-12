// Copyright 2024 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.Executor;



/**
 * {@link Executor} that automatically transfers active {@link InjectionContext Context}s using its
 * associated {@link #getContextBinder() ContextBinder} when executing tasks.
 * <p>
 * If {@code ContextTrackingExecutor}s need to be created before an
 * {@link com.google.inject.Injector}, then an instance of {@link ContextBinder} may be obtained
 * with {@link ScopeModule#newContextBinder()}. Libs derived from {@code guice-context-scopes}
 * usually create a {@code public final ContextBinder} field in their concrete subclasses of
 * {@link ScopeModule}.</p>
 */
public interface ContextTrackingExecutor extends Executor {



	/**
	 * The underlying {@link Executor} that actually runs tasks.
	 * If this interface is implemented by some concrete {@link Executor} class (for example by some
	 * subclass of {@link java.util.concurrent.ThreadPoolExecutor}), then this method should simply
	 * return {@code this}. If this interface is implemented as a decorator wrapping some other
	 * {@link Executor}, this method should return this wrapped {@link Executor}.
	 */
	Executor getExecutor();



	/** The Associated {@link ContextBinder} that binds executed tasks to {@code Context}s. */
	ContextBinder getContextBinder();



	/**
	 * {@link ContextBinder#bindToContext(Runnable) Binds} {@code task} to active
	 * {@link InjectionContext Context}s using {@link #getContextBinder() the associated Binder} and
	 * sends it to {@link #getExecutor() the underlying Executor}.
	 */
	@Override
	default void execute(Runnable task) {
		getExecutor().execute(getContextBinder().bindToContext(task));
	}



	/** Wraps {@code executorToWrap} with a {@link ContextTrackingExecutor} decorator. */
	static ContextTrackingExecutor of(Executor executorToWrap, ContextBinder ctxBinder) {
		return new ContextTrackingExecutor() {
			@Override public Executor getExecutor() {
				return executorToWrap;
			}
			@Override public ContextBinder getContextBinder() {
				return ctxBinder;
			}
			@Override public String toString() {
				return "ContextTrackingExecutor { wrappedExecutor = " + getExecutor() + " }";
			}
		};
	}
}

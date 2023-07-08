// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.*;



/**
 * Scopes objects to the current context obtained from the associated {@link ContextTracker}.
 */
public class ContextScope<CtxT extends TrackableContext<CtxT>> implements Scope {



	protected final ContextTracker<CtxT> tracker;

	public String getName() { return name; }
	final String name;



	public ContextScope(String name, ContextTracker<CtxT> tracker) {
		this.name = name;
		this.tracker = tracker;
	}



	/**
	 * @throws RuntimeException if there's no context for the current thread. This most commonly
	 * happens when providing a callback to some async method without transferring the context. Use
	 * static helper methods {@link ContextTrackingExecutor#getActiveContexts(java.util.List)}
	 * and {@link ContextTrackingExecutor#executeWithinAll(java.util.List, Runnable)} to fix it:
	 * <pre>
	 * class MyClass {
	 *
	 *     &commat;Inject ContextTracker&lt;ContextT1&gt; tracker1;
	 *     &commat;Inject ContextTracker&lt;ContextT2&gt; tracker2;
	 *
	 *     void myMethod(Object param) {
	 *         // myMethod code
	 *         var activeCtxList = ContextTrackingExecutor.getActiveContexts(tracker1, tracker2);
	 *         someAsyncMethod(param, (callbackParam) -&gt;
	 *             ContextTrackingExecutor.executeWithinAll(activeCtxList, () -&gt; {
	 *                 // callback code
	 *             }
	 *         ));
	 *     }
	 * }</pre>
	 */
	@Override
	public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
		return new ScopedProvider<>(key, unscoped);
	}

	public class ScopedProvider<T> implements Provider<T> {

		final Key<T> key;
		final Provider<T> unscoped;



		private ScopedProvider(Key<T> key, Provider<T> unscoped) {
			this.key = key;
			this.unscoped = unscoped;
		}



		@Override public T get() {
			try {
				return getContext().provideIfAbsent(key, unscoped);
			} catch (NullPointerException e) {
				// result of a bug that will be fixed in development phase: don't check manually
				// in production each time.
				throw new RuntimeException("no context for thread "
						+ Thread.currentThread().getName() + " in scope " + name
						+ ". See javadoc for ContextScope.scope(...)");
			}
		}



		@Override public String toString() {
			return "ScopedProvider { scope=\"" + name + "\", key=" + key + ", unscoped=" + unscoped
					+ " }";
		}
	}



	/**
	 * Returns a context instance from which scoped objects should be obtained by this Scope. By
	 * default returns directly the context obtained from {@link #tracker}. May be overridden to
	 * return some context induced by the one from the {@link #tracker}.
	 * @see InducedContextScope
	 */
	protected InjectionContext getContext() {
		return tracker.getCurrentContext();
	}



	@Override
	public String toString() {
		return name;
	}
}

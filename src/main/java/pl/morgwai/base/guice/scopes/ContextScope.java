// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;



/**
 * Scopes objects to a call context obtained from the associated {@link ContextTracker}.
 */
public class ContextScope<Ctx extends ServerSideContext<Ctx>> implements Scope {



	final ContextTracker<Ctx> tracker;

	final String name;
	public String getName() { return name; }



	/**
	 * @throws RuntimeException if there's no context for the current thread. This most commonly
	 * happens when providing a callback to some async method without transferring the context.
	 * Use static helper methods {@link ContextTrackingExecutor#getActiveCount()} and
	 * {@link ContextTrackingExecutor#executeWithinAll(java.util.List, Runnable)} to fix it:
	 * <Pre>
	 *class MyClass {
	 *
	 *    &commat;Inject ContextTracker&lt;ContextT1&gt; tracker1;
	 *    &commat;Inject ContextTracker&lt;ContextT2&gt; tracker2;
	 *
	 *    void myMethod(Object param) {
	 *        // myMethod code
	 *        var activeCtxList = ContextTrackingExecutor.getActiveContexts(tracker1, tracker2);
	 *        someAsyncMethod(param, (callbackParam) -&gt;
	 *            ContextTrackingExecutor.executeWithinAll(activeCtxList, () -&gt; {
	 *                // callback code
	 *            }
	 *        ));
	 *    }
	 *}</pre>
	 */
	@Override
	public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
		return () -> {
			try {
				return tracker.getCurrentContext().provideAttributeIfAbsent(key, unscoped);
			} catch (NullPointerException e) {
				// NPE here is a result of a bug that will be usually eliminated in development
				// phase and not happen in production, so we catch NPE instead of checking manually
				// each time.
				throw new RuntimeException("no context for thread "
						+ Thread.currentThread().getName() + " in scope " + name
						+ ". See javadoc for ContextScope.scope(...)");
			}
		};
	}



	public ContextScope(String name, ContextTracker<Ctx> tracker) {
		this.name = name;
		this.tracker = tracker;
	}



	@Override
	public String toString() {
		return name;
	}
}

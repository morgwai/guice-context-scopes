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
	 * This can be fixed similarly to the below code:
	 * <Pre>
	 *class MyClass {
	 *
	 *    &commat;Inject ContextTracker&lt;ContextT&gt; tracker;
	 *
	 *    void myMethod(Object param) {
	 *        // myMethod code
	 *        var ctx = tracker.getCurrentContext();
	 *        someAsyncMethod(param, (callbackParam) ->
	 *            ctx.executeWithinSelf(() -> {
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

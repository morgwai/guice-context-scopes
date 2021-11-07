// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;



/**
 * Scopes objects to a context obtained from the associated {@link ContextTracker}.
 */
public class ContextScope<CtxT extends TrackableContext<CtxT>> implements Scope {



	final ContextTracker<CtxT> tracker;

	final String name;
	public String getName() { return name; }



	public ContextScope(String name, ContextTracker<CtxT> tracker) {
		this.name = name;
		this.tracker = tracker;
	}



	/**
	 * @throws RuntimeException if there's no context for the current thread. This most commonly
	 * happens when providing a callback to some async method without transferring the context. Use
	 * static helper methods {@link ContextTrackingExecutor#getActiveContexts(List)}
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
		return () -> {
			try {
				return getContext().provideIfAbsent(key, unscoped);
			} catch (NullPointerException e) {
				// result of a bug that will be fixed in development phase: don't check manually
				// in production each time.
				throw new RuntimeException("no context for thread "
						+ Thread.currentThread().getName() + " in scope " + name
						+ ". See javadoc for ContextScope.scope(...)");
			}
		};
	}



	protected ServerSideContext getContext() {
		return tracker.getCurrentContext();
	}



	@Override
	public String toString() {
		return name;
	}
}

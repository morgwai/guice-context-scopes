// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.*;



/** Scopes objects to the current context obtained from the associated {@link ContextTracker}. */
public class ContextScope<CtxT extends TrackableContext<CtxT>> implements Scope {



	protected final ContextTracker<CtxT> tracker;

	public String getName() { return name; }
	final String name;



	public ContextScope(String name, ContextTracker<CtxT> tracker) {
		this.name = name;
		this.tracker = tracker;
	}



	/**
	 * See {@link Scope#scope(Key, Provider)}.
	 * @return a {@link ScopedProvider}.
	 */
	@Override
	public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
		return new ScopedProvider<>(key, unscoped);
	}

	/**
	 * Returned by {@link #scope(Key, Provider)}, provides objects obtained from the current
	 * context.
	 */
	public class ScopedProvider<T> implements Provider<T> {

		final Key<T> key;
		final Provider<T> unscoped;



		private ScopedProvider(Key<T> key, Provider<T> unscoped) {
			this.key = key;
			this.unscoped = unscoped;
		}



		/**
		 * Provides an object scoped to {@link #getContext() the current context}.
		 * @throws OutOfScopeException if the current thread is running outside of any context. This
		 *     most commonly happens if an async task was not wrapped with {@link ContextBoundTask}
		 *     before passing to an executor or when providing a callback to an async method
		 *     without transferring contexts. Use static helper methods
		 *     {@link ContextTracker#getActiveContexts(java.util.List)} and
		 *     {@link TrackableContext#executeWithinAll(java.util.List, Runnable)} to transfer
		 *     contexts manually in callbacks and {@link ContextBoundTask} decorator when passing
		 *     tasks to {@link java.util.concurrent.Executor#execute(Runnable)}:
		 *     <pre>
		 * class MyClass {
		 *
		 *     // deriving libraries usually bind List&lt;ContextTracker&lt;?&gt;&gt; appropriately
		 *     &#64;Inject List&lt;ContextTracker&lt;?&gt;&gt; allTrackers;
		 *
		 *     void methodThatCallsSomeAsyncMethod(...) {
		 *         // other code here...
		 *         final var activeCtxs = ContextTracker.getActiveContexts(allTrackers);
		 *         someAsyncMethod(arg1, ... argN, (callbackParam) -&gt;
		 *             TrackableContext.executeWithinAll(activeCtxs, () -&gt; {
		 *                 // callback code here...
		 *             })
		 *         );
		 *     }
		 *
		 *     void methodThatUsesSomeExecutor(...) {
		 *         Runnable myTask;
		 *         // build myTask here...
		 *         myExecutor.execute(new ContextBoundTask(
		 *                 myTask, ContextTracker.getActiveContexts(allTrackers)));
		 *     }
		 *
		 *     // other stuff of MyClass here...
		 * }</pre>
		 */
		@Override public T get() {
			try {
				return getContext().provideIfAbsent(key, unscoped);
			} catch (NullPointerException e) {
				// result of a bug that will be fixed in development phase: don't check manually
				// in production each time.
				throw new OutOfScopeException("no context for thread "
						+ Thread.currentThread().getName() + " in scope " + name
						+ ". See the javadoc for ContextScope.ScopedProvider.get()");
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

// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;



/**
 * Binds closures to contexts that were active at the time of binding. Deriving libs should bind
 * this class in their main {@link com.google.inject.Module}. This can later be used to transfer
 * contexts when passing callbacks to async functions:
 * <pre>
 * class MyComponent {
 *
 *     &#64;Inject ContextBinder ctxBinder;
 *
 *     void methodThatCallsSomeAsyncMethod(...) {
 *         // other code here...
 *         someAsyncMethod(arg1, ... arN, ctxBinder.bindToContext((callbackParam) -&gt; {
 *             // callback code here...
 *         }));
 *     }
 * }</pre>
 */
public class ContextBinder {



	final List<ContextTracker<?>> trackers;



	public ContextBinder(List<ContextTracker<?>> trackers) {
		this.trackers = trackers;
	}



	public ContextBoundTask bindToContext(Runnable toBind) {
		return new ContextBoundTask(ContextTracker.getActiveContexts(trackers), toBind);
	}



	public <T> ContextBoundConsumer<T> bindToContext(Consumer<T> toBind) {
		return new ContextBoundConsumer<>(ContextTracker.getActiveContexts(trackers), toBind);
	}



	public <T, U> ContextBoundBiConsumer<T, U> bindToContext(BiConsumer<T, U> toBind) {
		return new ContextBoundBiConsumer<>(ContextTracker.getActiveContexts(trackers), toBind);
	}
}

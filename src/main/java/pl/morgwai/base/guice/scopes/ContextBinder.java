// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.*;



/**
 * Binds closures to contexts that were active at the time of binding. This can be used to transfer
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
 * <p>
 * Deriving libs should bind this class in their main {@link com.google.inject.Module}.
 * </p>
 */
public class ContextBinder {



	final List<ContextTracker<?>> trackers;



	public ContextBinder(List<ContextTracker<?>> trackers) {
		this.trackers = trackers;
	}



	public ContextBoundRunnable bindToContext(Runnable toBind) {
		return new ContextBoundRunnable(ContextTracker.getActiveContexts(trackers), toBind);
	}



	public <T> ContextBoundConsumer<T> bindToContext(Consumer<T> toBind) {
		return new ContextBoundConsumer<>(ContextTracker.getActiveContexts(trackers), toBind);
	}



	public <T, U> ContextBoundBiConsumer<T, U> bindToContext(BiConsumer<T, U> toBind) {
		return new ContextBoundBiConsumer<>(ContextTracker.getActiveContexts(trackers), toBind);
	}



	public <T> ContextBoundCallable<T> bindToContext(Callable<T> toBind) {
		return new ContextBoundCallable<>(ContextTracker.getActiveContexts(trackers), toBind);
	}



	public <T, R> ContextBoundFunction<T, R> bindToContext(Function<T, R> toBind) {
		return new ContextBoundFunction<>(ContextTracker.getActiveContexts(trackers), toBind);
	}



	public <T, U, R> ContextBoundBiFunction<T, U, R> bindToContext(BiFunction<T, U, R> toBind) {
		return new ContextBoundBiFunction<>(ContextTracker.getActiveContexts(trackers), toBind);
	}
}

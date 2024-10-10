// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.*;

import com.google.inject.Inject;

import static pl.morgwai.base.guice.scopes.ContextTracker.getActiveContexts;



/**
 * Binds closures to {@link InjectionContext Contexts} that were active at the time of a given
 * binding.
 * This can be used to transfer {@code Contexts} when passing callbacks to async functions:
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



	protected final List<ContextTracker<?>> trackers;



	@Inject
	public ContextBinder(List<ContextTracker<?>> trackers) {
		this.trackers = trackers;
	}



	public ContextBoundRunnable bindToContext(Runnable toBind) {
		return new ContextBoundRunnable(getActiveContexts(trackers), toBind);
	}



	public <T> ContextBoundConsumer<T> bindToContext(Consumer<T> toBind) {
		return new ContextBoundConsumer<>(getActiveContexts(trackers), toBind);
	}



	public <T, U> ContextBoundBiConsumer<T, U> bindToContext(BiConsumer<T, U> toBind) {
		return new ContextBoundBiConsumer<>(getActiveContexts(trackers), toBind);
	}



	public <T> ContextBoundCallable<T> bindToContext(Callable<T> toBind) {
		return new ContextBoundCallable<>(getActiveContexts(trackers), toBind);
	}



	public <T, R> ContextBoundFunction<T, R> bindToContext(Function<T, R> toBind) {
		return new ContextBoundFunction<>(getActiveContexts(trackers), toBind);
	}



	public <T, U, R> ContextBoundBiFunction<T, U, R> bindToContext(BiFunction<T, U, R> toBind) {
		return new ContextBoundBiFunction<>(getActiveContexts(trackers), toBind);
	}



	// Callable and Supplier have indistinguishable lambdas, hence different method name
	public <T> ContextBoundSupplier<T> bindSupplierToContext(Supplier<T> toBind) {
		return new ContextBoundSupplier<>(getActiveContexts(trackers), toBind);
	}
}

// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.*;

import pl.morgwai.base.function.Throwing5Task;

import static pl.morgwai.base.guice.scopes.ContextTracker.getActiveContexts;



/**
 * Binds closures to {@link InjectionContext Contexts} that were active at the time of a given
 * binding.
 * This can be used to transfer {@code Contexts} semi-automatically when switching {@code Thread}s,
 * for example when passing callbacks to async functions:
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



	public ContextBinder(List<ContextTracker<?>> trackers) {
		this.trackers = trackers;
	}



	public ContextBoundRunnable bindToContext(Runnable runnableToBind) {
		return new ContextBoundRunnable(getActiveContexts(trackers), runnableToBind);
	}



	public <T> ContextBoundConsumer<T> bindToContext(Consumer<T> consumerToBind) {
		return new ContextBoundConsumer<>(getActiveContexts(trackers), consumerToBind);
	}



	public <T, U> ContextBoundBiConsumer<T, U> bindToContext(BiConsumer<T, U> biConsumerToBind) {
		return new ContextBoundBiConsumer<>(getActiveContexts(trackers), biConsumerToBind);
	}



	public <T> ContextBoundCallable<T> bindToContext(Callable<T> callableToBind) {
		return new ContextBoundCallable<>(getActiveContexts(trackers), callableToBind);
	}



	public <T, R> ContextBoundFunction<T, R> bindToContext(Function<T, R> functionToBind) {
		return new ContextBoundFunction<>(getActiveContexts(trackers), functionToBind);
	}



	public <T, U, R> ContextBoundBiFunction<T, U, R> bindToContext(
		BiFunction<T, U, R> biFunctionToBind
	) {
		return new ContextBoundBiFunction<>(getActiveContexts(trackers), biFunctionToBind);
	}



	// Callable and Supplier have indistinguishable lambdas, hence a different method name
	public <T> ContextBoundSupplier<T> bindSupplierToContext(Supplier<T> supplierToBind) {
		return new ContextBoundSupplier<>(getActiveContexts(trackers), supplierToBind);
	}



	// Callable and ThrowingTask have indistinguishable lambdas, hence a different method name
	public <
		R,
		E1 extends Exception,
		E2 extends Exception,
		E3 extends Exception,
		E4 extends Exception,
		E5 extends Exception
	> ContextBoundThrowingTask<R, E1, E2, E3, E4, E5> bindTaskToContext(
		Throwing5Task<R, E1, E2, E3, E4, E5> taskToBind
	) {
		return new ContextBoundThrowingTask<>(getActiveContexts(trackers), taskToBind);
	}
}

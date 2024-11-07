// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.*;

import pl.morgwai.base.function.*;

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



	public <
		E1 extends Throwable, E2 extends Throwable, E3 extends Throwable, E4 extends Throwable
	> ContextBoundThrowingTask<E1, E2, E3, E4> bindToContext(
		Throwing4Task<E1, E2, E3, E4> taskToBind
	) {
		return new ContextBoundThrowingTask<>(getActiveContexts(trackers), taskToBind);
	}



	public <
		R, E1 extends Throwable, E2 extends Throwable, E3 extends Throwable, E4 extends Throwable
	> ContextBoundThrowingComputation<R, E1, E2, E3, E4> bindToContext(
		Throwing4Computation<R, E1, E2, E3, E4> computationToBind
	) {
		return new ContextBoundThrowingComputation<>(
			getActiveContexts(trackers),
			computationToBind
		);
	}



	public <R> ContextBoundThrowingComputation<
		R, Exception, RuntimeException, RuntimeException, RuntimeException
	> bindToContext(Callable<R> callableToBind) {
		return new ContextBoundThrowingComputation<>(
			getActiveContexts(trackers),
			ThrowingComputation.of(callableToBind)
		);
	}



	public <T> ContextBoundConsumer<T> bindToContext(Consumer<T> consumerToBind) {
		return new ContextBoundConsumer<>(getActiveContexts(trackers), consumerToBind);
	}



	public <T, U> ContextBoundBiConsumer<T, U> bindToContext(BiConsumer<T, U> biConsumerToBind) {
		return new ContextBoundBiConsumer<>(getActiveContexts(trackers), biConsumerToBind);
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
}

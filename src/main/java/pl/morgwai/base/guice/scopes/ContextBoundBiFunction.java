// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.function.BiFunction;



/** A decorator that executes its wrapped {@link BiFunction} within supplied {@code Contexts}. */
public class ContextBoundBiFunction<T, U, R> extends ContextBoundClosure<BiFunction<T, U, R>>
		implements BiFunction<T, U, R> {



	protected ContextBoundBiFunction(
			List<TrackableContext<?>> contexts, BiFunction<T, U, R> biFunctionToBind) {
		super(contexts, biFunctionToBind);
	}



	@Override
	public R apply(T param1, U param2) {
		try {
			return TrackableContext.executeWithinAll(
				contexts,
				new CallableWrapper<R>() {
					@Override public R call() {
						return boundClosure.apply(param1, param2);
					}
				}
			);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception neverHappens) {  // result of wrapping boundClosure with a Callable
			throw new RuntimeException(neverHappens);
		}
	}
}

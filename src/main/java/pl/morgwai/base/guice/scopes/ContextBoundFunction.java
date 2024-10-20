// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.function.Function;



/** Executes its wrapped {@link Function} within supplied {@code Contexts}. */
public class ContextBoundFunction<T, R> extends ContextBoundClosure<Function<T, R>>
		implements Function<T, R> {



	public ContextBoundFunction(List<TrackableContext<?>> contexts, Function<T, R> functionToBind) {
		super(contexts, functionToBind);
	}



	@Override
	public R apply(T param) {
		try {
			return TrackableContext.executeWithinAll(
				contexts,
				new CallableWrapper<>(() -> boundClosure.apply(param))
			);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception neverHappens) {  // result of wrapping boundClosure with a Callable
			throw new RuntimeException(neverHappens);
		}
	}
}

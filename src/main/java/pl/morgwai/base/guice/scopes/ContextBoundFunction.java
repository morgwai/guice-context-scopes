// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.function.Function;

import static pl.morgwai.base.guice.scopes.ThrowingTask.newThrowingTask;



/** Executes its wrapped {@link Function} within supplied {@code Contexts}. */
public class ContextBoundFunction<T, R> extends ContextBoundClosure<Function<T, R>>
		implements Function<T, R> {



	public ContextBoundFunction(List<TrackableContext<?>> contexts, Function<T, R> functionToBind) {
		super(contexts, functionToBind);
	}



	@Override
	public R apply(T param) {
		return TrackableContext.executeWithinAll(
			contexts,
			newThrowingTask(boundClosure, param)
		);
	}
}

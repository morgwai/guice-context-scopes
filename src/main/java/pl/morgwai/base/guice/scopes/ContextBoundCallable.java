// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;

import static pl.morgwai.base.function.ThrowingTask.newThrowingTask;



/** Executes its wrapped {@link Callable} within supplied {@code Contexts}. */
public class ContextBoundCallable<T> extends ContextBoundClosure<Callable<T>> implements Callable<T>
{



	public ContextBoundCallable(List<TrackableContext<?>> contexts, Callable<T> taskToBind) {
		super(contexts, taskToBind);
	}



	@Override
	public T call() throws Exception {
		return TrackableContext.executeWithinAll(
			contexts,
			newThrowingTask(boundClosure)
		);
	}
}

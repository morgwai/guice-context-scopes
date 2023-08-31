// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;



/** A decorator that will execute its wrapped {@link Callable} within supplied contexts. */
public class ContextBoundCallable<T> extends ContextBoundClosure<Callable<T>> implements Callable<T>
{



	public ContextBoundCallable(List<TrackableContext<?>> contexts, Callable<T> taskToBind) {
		super(contexts, taskToBind);
	}



	@Override
	public T call() throws Exception {
		return TrackableContext.executeWithinAll(contexts, boundClosure);
	}
}

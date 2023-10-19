// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;



/** A decorator that executes its wrapped {@link Runnable} within supplied {@code Contexts}. */
public class ContextBoundRunnable extends ContextBoundClosure<Runnable> implements Runnable {



	public ContextBoundRunnable(List<TrackableContext<?>> contexts, Runnable taskToBind) {
		super(contexts, taskToBind);
	}



	@Override
	public void run() {
		TrackableContext.executeWithinAll(contexts, boundClosure);
	}
}

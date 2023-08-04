// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;



/** A {@link Runnable task} that will be executed within supplied contexts. */
public class ContextBoundTask implements Runnable {



	public List<TrackableContext<?>> getContexts() { return contexts; }
	final List<TrackableContext<?>> contexts;

	public Runnable getWrappedTask() { return wrappedTask; }
	final Runnable wrappedTask;



	public ContextBoundTask(List<TrackableContext<?>> contexts, Runnable taskToBind) {
		this.contexts = contexts;
		this.wrappedTask = taskToBind;
	}



	@Override
	public void run() {
		TrackableContext.executeWithinAll(contexts, wrappedTask);
	}



	@Override
	public String toString() {
		return "ContextBoundTask { task = " + wrappedTask + " }";
	}
}

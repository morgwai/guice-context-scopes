/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.Callable;



/**
 * A context that can be tracked via an associated {@link ContextTracker} and that allows to run
 * operations within itself.
 */
public abstract class TrackableContext<Ctx extends TrackableContext<Ctx>> extends ServerSideContext{



	ContextTracker<Ctx> tracker;



	@SuppressWarnings("unchecked")
	public void runWithinSelf(Runnable operation) {
		tracker.setCurrentContext((Ctx) this);
		try {
			operation.run();
		} finally {
			tracker.clearCurrentContext();
		}
	}



	@SuppressWarnings("unchecked")
	public <T> T callWithinSelf(Callable<T> operation) throws Exception {
		tracker.setCurrentContext((Ctx) this);
		try {
			return operation.call();
		} finally {
			tracker.clearCurrentContext();
		}
	}



	protected TrackableContext(ContextTracker<Ctx> tracker) {
		this.tracker = tracker;
	}
}

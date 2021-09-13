// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.Callable;



/**
 * Allows to track which server-side call is handled by which thread.
 */
public class ContextTracker<Ctx extends ServerSideContext<Ctx>> {



	private final ThreadLocal<Ctx> currentContex = new ThreadLocal<>();



	/**
	 * @return context which the calling thread is running within.
	 */
	public Ctx getCurrentContext() {
		return currentContex.get();
	}



	void trackWhileExecuting(Ctx ctx, Runnable operation) {
		currentContex.set(ctx);
		try {
			operation.run();
		} finally {
			currentContex.remove();
		}
	}



	<T> T trackWhileExecuting(Ctx ctx, Callable<T> operation) throws Exception {
		currentContex.set(ctx);
		try {
			return operation.call();
		} finally {
			currentContex.remove();
		}
	}
}

/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.Callable;



/**
 * A {@link ContextTracker} that uses <code>ThreadLocal</code> to store contexts.
 */
public class ThreadLocalContextTracker<Ctx extends ServerCallContext>
		implements ContextTracker<Ctx> {



	private final ThreadLocal<Ctx> contexts = new ThreadLocal<>();



	@Override
	public Ctx getCurrentContext() {
		return contexts.get();
	}



	@Override
	public void setCurrentContext(Ctx ctx) {
		contexts.set(ctx);
	}



	@Override
	public void clearCurrentContext() {
		contexts.remove();
	}



	@Override
	public void runWithin(Ctx ctx, Runnable operation) {
		contexts.set(ctx);
		operation.run();
		contexts.remove();
	}



	@Override
	public <T> T callWithin(Ctx ctx, Callable<T> operation) throws Exception {
		contexts.set(ctx);
		try {
			return operation.call();
		} finally {
			contexts.remove();
		}
	}
}

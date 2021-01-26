/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;



/**
 * A {@link ContextTracker} that uses <code>ThreadLocal</code> to store contexts.
 */
public class ThreadLocalContextTracker<Ctx extends ServerCallContext>
		implements ContextTracker<Ctx> {



	private final ThreadLocal<Ctx> contexts = new ThreadLocal<>();



	@Override
	public void setCurrentContext(Ctx ctx) {
		contexts.set(ctx);
	}



	@Override
	public Ctx getCurrentContext() {
		return contexts.get();
	}



	@Override
	public void clearCurrentContext() {
		contexts.remove();
	}
}

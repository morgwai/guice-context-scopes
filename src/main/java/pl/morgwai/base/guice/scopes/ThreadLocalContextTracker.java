/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;



/**
 * A {@link ContextTracker} that uses <code>ThreadLocal</code> to store contexts.
 */
public class ThreadLocalContextTracker<Ctx extends TrackableContext<Ctx>>
		extends ContextTracker<Ctx> {



	private final ThreadLocal<Ctx> contexts = new ThreadLocal<>();



	@Override
	public Ctx getCurrentContext() {
		return contexts.get();
	}



	@Override
	protected void setCurrentContext(Ctx ctx) {
		contexts.set(ctx);
	}



	@Override
	protected void clearCurrentContext() {
		contexts.remove();
	}
}

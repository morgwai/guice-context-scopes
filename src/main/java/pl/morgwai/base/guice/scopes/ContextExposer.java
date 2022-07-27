// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import com.google.inject.Key;
import com.google.inject.Provider;



/**
 * Intended for internal use by libs built on top of this one. Apps should obtain scoped objects via
 * Guice injections.
 */
public class ContextExposer<CtxT extends InjectionContext> {



	public CtxT getCtx() { return ctx; }
	final CtxT ctx;



	public ContextExposer(CtxT ctx) { this.ctx = ctx; }



	public void removeScopedObject(Key<?> key) { ctx.removeScopedObject(key); }



	public <T> T provideIfAbsent(Key<T> key, Provider<T> provider) {
		return ctx.provideIfAbsent(key, provider);
	}
}

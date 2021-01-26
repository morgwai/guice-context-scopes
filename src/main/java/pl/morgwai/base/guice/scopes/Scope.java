/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;

import com.google.inject.Key;



/**
 * A <code>Scope</code> that supports removing objects saved by scoped providers.
 */
public interface Scope extends com.google.inject.Scope {

	<T> void removeObjectFromScope (Key<T> key);
}

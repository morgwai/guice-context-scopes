// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
/**
 * Classes for building Guice {@link com.google.inject.Scope}s, that get automatically transferred
 * when dispatching work to other threads.
 * <h3>Code organization guidelines for deriving libs</h3>
 * <p>
 * Deriving libs should usually define their "central class" containing instances of
 * {@link pl.morgwai.base.guice.scopes.ContextTracker}s and
 * {@link pl.morgwai.base.guice.scopes.ContextScope}s for all defined
 * {@link pl.morgwai.base.guice.scopes.TrackableContext} subclasses as instance vars.</p>
 * <p>
 * For convenience such central class should also be a {@link com.google.inject.Module} that defines
 * the below infrastructure bindings in its
 * {@link com.google.inject.Module#configure(com.google.inject.Binder) configure(binder)} method:
 * </p>
 * <ul>
 *   <li>{@code List<ContextTracker<?>>} to a {@code  List} of all defined {@code ContextTracker}
 *       instances</li>
 *   <li>Their respective types to its defined individual {@code  ContextTracker} instances</li>
 *   <li>Their respective types to {@link com.google.inject.Provider}s of its defined
 *       {@code InjectionContext} instances, usually defined similarly to 1 of the below:<br/>
 *       {@code respectiveTrackerInstance::getCurrentContext}<br/>
 *       or<br/>
 *       {@code () -> respectiveTrackerInstance.getCurrentContext().getRespectiveInducedContext()}
 *       </li>
 *   <li>{@link pl.morgwai.base.guice.scopes.ContextBinder} class to an instances created by passing
 *       a {@code  List} of all defined {@code ContextTracker} instances to
 *       {@link pl.morgwai.base.guice.scopes.ContextBinder#ContextBinder(java.util.List) its
 *       constructor}</li>
 * </ul>
 * <p>
 * The central {@code Module} should have a {@code boolean} constructor param named
 * {@code disableCircularProxies}, that will be passed to
 * {@link pl.morgwai.base.guice.scopes.InjectionContext#InjectionContext(boolean) Context
 * constructors}. Additionally, if its value is {@code true}, then
 * {@link com.google.inject.Module#configure(com.google.inject.Binder) configure(binder)} method
 * should call {@link com.google.inject.Binder#disableCircularProxies()}.</p>
 * @see <a href='https://github.com/morgwai/guice-context-scopes#guice-context-scopes'>
 *     project homepage</a>
 */
package pl.morgwai.base.guice.scopes;

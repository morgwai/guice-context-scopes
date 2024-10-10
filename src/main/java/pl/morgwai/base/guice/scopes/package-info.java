// Copyright 2021 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
/**
 * Classes for building Guice {@link com.google.inject.Scope}s, that get automatically transferred
 * when dispatching work to other threads.
 * <h3>Code organization guidelines for deriving libs:</h3>
 * <p>
 * Deriving libs should usually define their "central class" extending
 * {@link com.google.inject.Module} containing the following:</p>
 * <ul>
 *     <li>instances of {@link pl.morgwai.base.guice.scopes.ContextTracker}s for all defined
 *         {@link pl.morgwai.base.guice.scopes.TrackableContext} subclasses</li>
 *     <li>instances of corresponding {@link pl.morgwai.base.guice.scopes.ContextScope}s</li>
 *     <li>{@code List<ContextTracker<?>> allTrackers} instance var containing all the above
 *         {@link pl.morgwai.base.guice.scopes.ContextTracker} instances</li>
 *     <li>{@link pl.morgwai.base.guice.scopes.ContextBinder}
 *         {@code contextBinder = new ContextBinder(allTrackers)} instance var</li>
 *     <li>{@code getActiveContexts()} method returning
 *         {@code ContextTracker.getActiveContexts(allTrackers)}</li>
 *     <li>
 *         {@link com.google.inject.Module#configure(com.google.inject.Binder) configure(binder)}
 *         method that creates the following bindings:
 *         <ul>
 *             <li>{@code List<ContextTracker<?>>} to {@code  allTrackers}</li>
 *             <li>Their respective types to the defined {@code  ContextTracker} instances</li>
 *             <li>Their respective types to {@link com.google.inject.Provider}s of the defined
 *                 {@code Context} instances, usually defined similarly to 1 of the below:
 *                 <ul>
 *                     <li>for {@link pl.morgwai.base.guice.scopes.TrackableContext}s:
 *                         {@code respectiveTrackerInstance::getCurrentContext}</li>
 *                     <li>for induced {@link pl.morgwai.base.guice.scopes.InjectionContext}s:
 *                         {@code () ->
 *                         inducingContextTracker.getCurrentContext().getRespectiveInducedContext()}
 *                     </li>
 *                 </ul>
 *             </li>
 *         </ul>
 *     </li>
 * </ul>
 * @see <a href='https://github.com/morgwai/guice-context-scopes#guice-context-scopes'>
 *     project homepage</a>
 */
package pl.morgwai.base.guice.scopes;

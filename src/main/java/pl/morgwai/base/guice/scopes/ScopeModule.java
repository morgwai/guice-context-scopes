// Copyright 2024 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

import com.google.inject.*;
import com.google.inject.Module;



/**
 * Base class for central {@link Module}s of libs derived from {@code guice-context-scopes}.
 * Derived libs should define a concrete subclass containing the following {@code public final}
 * fields:<ol>
 *   <li>{@link ContextScope} fields for all defined {@link TrackableContext} subclasses,
 *       initialized with {@link #newContextScope(String, Class)} calls.</li>
 *   <li>{@link InducedContextScope} fields for all defined {@link InjectionContext} subclasses that
 *       are induced by some {@link TrackableContext} subclass, initialized with
 *       {@link #newInducedContextScope(String, Class, ContextTracker, Function)} calls.</li>
 *   <li>{@link ContextBinder} field for app-level development convenience, initialized with a
 *       {@link #newContextBinder()} call. This may be useful for app developers when creating their
 *       global {@link ContextTrackingExecutor} instances bound for injection with
 *       {@link com.google.inject.binder.LinkedBindingBuilder#toInstance(Object)
 *       toInstance(myGlobalCtxTrackingExecutor)} calls in their {@link Module}s.</li>
 * </ol>
 * <p>
 * App developers should then create a global instance of such {@code ScopeModule} subclass,
 * pass its {@link Scope}s to their other {@link Module}s (as needed for scoping of their app
 * components) and finally pass this {@code ScopeModule} instance to their
 * {@link Guice#createInjector(Module...)} call(s) along with their other {@link Module}s.</p>
 * @see <a href="https://github.com/morgwai/guice-context-scopes#developing-portable-modules">
 *     Developing portable Modules</a>
 */
public abstract class ScopeModule implements Module {



	final Map<Class<? extends TrackableContext<?>>, ContextTracker<?>> trackableCtxs =
			new HashMap<>(1);
	final Map<Class<? extends InjectionContext>, ContextTracker<?>> inducedCtxs = new HashMap<>(3);
	final Map<
		Class<? extends InjectionContext>,
		Function<? extends TrackableContext<?>, ? extends InjectionContext>
	> inducedCtxRetrievers = new HashMap<>(3);



	/**
	 * Creates a new {@link ContextScope} {@link ContextScope#name named} {@code name} together with
	 * a {@link ContextTracker} for the {@link TrackableContext} subclass {@code ctxClass}.
	 * The newly created components are then added to internal structures, so that:<ul>
	 *   <li>The {@link ContextTracker} will be passed to {@link ContextBinder}s created with
	 *       {@link #newContextBinder()}.</li>
	 *   <li>{@link #configure(Binder)} will bind the {@link ContextTracker}'s type
	 *       ({@code ContextTracker<ContextT>}) to it.</li>
	 *   <li>{@link #configure(Binder)} will bind {@code ContextT} type to a {@link Provider}
	 *       based on the {@link ContextTracker#getCurrentContext()}.</li>
	 * </ul>
	 * <p>
	 * This method should usually be called to initialize {@code public final ContextScope} fields
	 * in subclasses of {@code ScopeModule}.</p>
	 * @return the newly created {@link ContextScope}. A reference to the corresponding
	 * {@link ContextTracker} may be obtained from {@link ContextScope#tracker}.
	 */
	protected <ContextT extends TrackableContext<ContextT>> ContextScope<ContextT> newContextScope(
		String name,
		Class<ContextT> ctxClass
	) {
		final var tracker = new ContextTracker<ContextT>();
		trackableCtxs.put(ctxClass, tracker);
		return new ContextScope<>(name, tracker);
	}



	/**
	 * Creates a new {@link InducedContextScope} {@link ContextScope#name named} {@code name} for
	 * the {@link InjectionContext} subclass {@code inducedCtxClass} induced by the
	 * {@link TrackableContext} {@code BaseContextT}.
	 * The newly created {@code InducedContextScope} will be added to internal structures, so that
	 * {@link #configure(Binder)} will bind {@code InducedContextT} type to a {@link Provider} based
	 * on {@code baseCtxTracker} and {@code inducedCtxRetriever}.
	 * <p>
	 * This method should usually be called to initialize {@code public final InducedContextScope}
	 * fields in subclasses of {@code ScopeModule}.</p>
	 */
	protected <
		BaseContextT extends TrackableContext<? super BaseContextT>,
		InducedContextT extends InjectionContext
	> InducedContextScope<BaseContextT, InducedContextT> newInducedContextScope(
		String name,
		Class<InducedContextT> inducedCtxClass,
		ContextTracker<BaseContextT> baseCtxTracker,
		Function<BaseContextT, InducedContextT> inducedCtxRetriever
	) {
		inducedCtxs.put(inducedCtxClass, baseCtxTracker);
		inducedCtxRetrievers.put(inducedCtxClass, inducedCtxRetriever);
		return new InducedContextScope<>(name, baseCtxTracker, inducedCtxRetriever);
	}

	/**
	 * Calls {@link #newInducedContextScope(String, Class, ContextTracker, Function)
	 * newInducedContextScope(name, inducedCtxClass, baseCtxScope.tracker, inducedCtxRetriever)}.
	 */
	protected <
		BaseContextT extends TrackableContext<? super BaseContextT>,
		InducedContextT extends InjectionContext
	> InducedContextScope<BaseContextT, InducedContextT> newInducedContextScope(
		String name,
		Class<InducedContextT> inducedCtxClass,
		ContextScope<BaseContextT> baseCtxScope,
		Function<BaseContextT, InducedContextT> inducedCtxRetriever
	) {
		return newInducedContextScope(
				name, inducedCtxClass, baseCtxScope.tracker, inducedCtxRetriever);
	}



	/**
	 * Returns all {@link ContextTracker}s created by previous
	 * {@link #newContextScope(String, Class)} calls.
	 */
	public List<ContextTracker<?>> getAllTrackers() {
		return List.copyOf(trackableCtxs.values());
	}



	/**
	 * Creates a new {@link ContextBinder} based on the result of {@link #getAllTrackers()}.
	 * <p>
	 * This method should usually be called to initialize a {@code public final ContextBinder} field
	 * in subclasses of {@code ScopeModule} <b>after</b> all
	 * {@link ContextScope} fields initializations with {@link #newContextScope(String, Class)}.</p>
	 */
	public ContextBinder newContextBinder() {
		return new ContextBinder(getAllTrackers());
	}



	/**
	 * Creates infrastructure bindings based on internal structures filled by
	 * {@link #newContextScope(String, Class)} and
	 * {@link #newInducedContextScope(String, Class, ContextTracker, Function)} calls.
	 * Specifically the following bindings are created:<ul>
	 *   <li>{@link ContextTracker#ALL_TRACKERS_KEY} to the result of
	 *       {@link #getAllTrackers()}.</li>
	 *   <li>{@link ContextBinder} type to an instance created with
	 *       {@link #newContextBinder()}.</li>
	 *   <li>Types of {@link TrackableContext}s for which {@link ContextScope}s were created with
	 *       {@link #newContextScope(String, Class)} to {@link Provider}s based on the corresponding
	 *       {@link ContextTracker}s.</li>
	 *   <li>Types of induced {@link InjectionContext}s for which {@link InducedContextScope}s were
	 *       created with {@link #newInducedContextScope(String, Class, ContextTracker, Function)}
	 *       to {@link Provider}s based on the {@link ContextTracker}s of the corresponding base
	 *       {@link TrackableContext}s and the respective corresponding retriever
	 *       {@link Function}s.</li>
	 * </ul>
	 */
	@Override
	public void configure(Binder binder) {
		binder.bind(ContextTracker.ALL_TRACKERS_KEY)
			.toInstance(List.copyOf(trackableCtxs.values()));
		binder.bind(ContextBinder.class)
			.toInstance(newContextBinder());
		for (var trackableCtxEntry: trackableCtxs.entrySet()) {
			bindTrackableCtx(binder, trackableCtxEntry.getKey(), trackableCtxEntry.getValue());
		}
		for (var inducedCtxEntry: inducedCtxs.entrySet()) {
			bindInducedCtx(binder, inducedCtxEntry.getKey(), inducedCtxEntry.getValue());
		}
	}

	<ContextT extends TrackableContext<ContextT>> void bindTrackableCtx(
		Binder binder,
		Class<? extends TrackableContext<?>> unboundCtxClass,
		ContextTracker<?> unboundTracker
	) {
		@SuppressWarnings("unchecked")
		final var ctxClass = (Class<ContextT>) unboundCtxClass;
		@SuppressWarnings("unchecked")
		final var tracker = (ContextTracker<ContextT>) unboundTracker;
		@SuppressWarnings("unchecked")
		final var trackerKey = (Key<ContextTracker<ContextT>>)
				Key.get(new ContextTrackerType(ctxClass));
		binder.bind(trackerKey).toInstance(tracker);
		binder.bind(ctxClass).toProvider(tracker::getCurrentContext);
	}

	<
		BaseContextT extends TrackableContext<? super BaseContextT>,
		InducedContextT extends InjectionContext
	> void bindInducedCtx(
		Binder binder,
		Class<InducedContextT> inducedCtxClass,
		ContextTracker<? extends TrackableContext<?>> unboundBaseCtxTracker
	) {
		@SuppressWarnings("unchecked")
		final var baseCtxTracker = (ContextTracker<BaseContextT>) unboundBaseCtxTracker;
		@SuppressWarnings("unchecked")
		final var inducedCtxRetriever = (Function<BaseContextT, InducedContextT>)
				inducedCtxRetrievers.get(inducedCtxClass);
		binder.bind(inducedCtxClass)
			.toProvider(() -> inducedCtxRetriever.apply(baseCtxTracker.getCurrentContext()));
	}



	static class ContextTrackerType implements ParameterizedType {

		final Type[] ctxClass;

		ContextTrackerType(Class<? extends TrackableContext<?>> ctxClass) {
			this.ctxClass = new Type[]{ctxClass};
		}

		@Override public Type[] getActualTypeArguments() {
			return ctxClass;
		}

		@Override public Type getRawType() { return ContextTracker.class; }

		@Override public Type getOwnerType() { return null; }  // top-level class

		/** Specified by {@link ParameterizedType} javadoc. */
		@Override public boolean equals(Object other) {
			if ( !(other instanceof ParameterizedType)) return false;
			final var otherType = (ParameterizedType) other;
			return ContextTracker.class.equals(otherType.getRawType())
					&& Arrays.equals(ctxClass, otherType.getActualTypeArguments());
		}

		/** Based on {@code sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl} source. */
		@Override public int hashCode() {
			return Arrays.hashCode(ctxClass) ^ ContextTracker.class.hashCode();
		}
	}
}

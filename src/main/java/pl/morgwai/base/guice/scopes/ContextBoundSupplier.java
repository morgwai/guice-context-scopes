// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.function.Supplier;



/** A decorator that executes its wrapped {@link Supplier} within supplied {@code Contexts}. */
public class ContextBoundSupplier<T> extends ContextBoundClosure<Supplier<T>>
		implements Supplier<T> {



	public ContextBoundSupplier(List<TrackableContext<?>> contexts, Supplier<T> supplierToBind) {
		super(contexts, supplierToBind);
	}



	@Override
	public T get() {
		try {
			return TrackableContext.executeWithinAll(
				contexts,
				new CallableWrapper<>(boundClosure::get)
			);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception neverHappens) {  // result of wrapping boundClosure with a Callable
			throw new RuntimeException(neverHappens);
		}
	}
}

// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.function.Supplier;

import static pl.morgwai.base.function.ThrowingTask.newThrowingTaskOfSupplier;



/** Executes its wrapped {@link Supplier} within supplied {@code Contexts}. */
public class ContextBoundSupplier<T> extends ContextBoundClosure<Supplier<T>>
		implements Supplier<T> {



	public ContextBoundSupplier(List<TrackableContext<?>> contexts, Supplier<T> supplierToBind) {
		super(contexts, supplierToBind);
	}



	@Override
	public T get() {
		return TrackableContext.executeWithinAll(
			contexts,
			newThrowingTaskOfSupplier(boundClosure)
		);
	}
}

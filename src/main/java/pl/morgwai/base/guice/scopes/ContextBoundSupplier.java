// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.function.Supplier;

import pl.morgwai.base.function.ThrowingComputation;



/** Executes its wrapped {@link Supplier} within supplied {@code Contexts}. */
public class ContextBoundSupplier<R> extends ContextBoundClosure<Supplier<R>>
		implements Supplier<R> {



	public ContextBoundSupplier(List<TrackableContext<?>> contexts, Supplier<R> supplierToBind) {
		super(contexts, supplierToBind);
	}



	@Override
	public R get() {
		return TrackableContext.executeWithinAll(
			contexts,
			ThrowingComputation.ofSupplier(boundClosure)
		);
	}
}

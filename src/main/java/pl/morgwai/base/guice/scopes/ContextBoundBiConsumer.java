// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.function.BiConsumer;



/** A decorator that will execute wrapped {@link BiConsumer} within supplied contexts. */
public class ContextBoundBiConsumer<T, U> extends ContextBoundClosure<BiConsumer<T, U>>
		implements BiConsumer<T, U> {



	public ContextBoundBiConsumer(
		List<TrackableContext<?>> contexts,
		BiConsumer<T, U> consumerToBind
	) {
		super(contexts, consumerToBind);
	}



	@Override
	public void accept(T param1, U param2) {
		TrackableContext.executeWithinAll(contexts, () -> boundClosure.accept(param1, param2));
	}
}

// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.function.Consumer;

import pl.morgwai.base.function.ThrowingTask;



/** Executes its wrapped {@link Consumer} within supplied {@code Contexts}. */
public class ContextBoundConsumer<T> extends ContextBoundClosure<Consumer<T>>
		implements Consumer<T> {



	public ContextBoundConsumer(List<TrackableContext<?>> contexts, Consumer<T> consumerToBind) {
		super(contexts, consumerToBind);
	}



	@Override
	public void accept(T param) {
		TrackableContext.executeWithinAll(
			contexts,
			ThrowingTask.of(boundClosure, param)
		);
	}
}

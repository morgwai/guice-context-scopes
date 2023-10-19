// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.function.Consumer;



/** A decorator that executes its wrapped {@link Consumer} within supplied {@code Contexts}. */
public class ContextBoundConsumer<T> extends ContextBoundClosure<Consumer<T>>
		implements Consumer<T> {



	public ContextBoundConsumer(List<TrackableContext<?>> contexts, Consumer<T> consumerToBind) {
		super(contexts, consumerToBind);
	}



	@Override
	public void accept(T param) {
		TrackableContext.executeWithinAll(
			contexts,
			new RunnableWrapper() {
				@Override public void run() {
					boundClosure.accept(param);
				}
			}
		);
	}
}

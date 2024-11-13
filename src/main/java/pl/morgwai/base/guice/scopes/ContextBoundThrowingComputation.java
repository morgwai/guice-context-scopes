// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;

import pl.morgwai.base.function.*;



/**
 * Executes its wrapped {@link ThrowingComputation} within supplied
 * {@link TrackableContext Contexts}.
 */
public class ContextBoundThrowingComputation<
	R, E1 extends Throwable, E2 extends Throwable, E3 extends Throwable, E4 extends Throwable
> extends ContextBoundClosure<Throwing4Computation<R, E1, E2, E3, E4>>
		implements Throwing4Computation<R, E1, E2, E3, E4> {



	public ContextBoundThrowingComputation(
		List<TrackableContext<?>> contexts,
		Throwing4Computation<R, E1, E2, E3, E4> taskToBind
	) {
		super(contexts, taskToBind);
	}



	@Override
	public R perform() throws E1, E2, E3, E4 {
		return TrackableContext.executeWithinAll(contexts, boundClosure);
	}
}

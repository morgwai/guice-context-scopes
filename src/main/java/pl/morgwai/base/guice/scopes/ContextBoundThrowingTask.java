// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.Callable;



/** Executes its wrapped {@link ThrowingTask} within supplied {@code Contexts}. */
public class ContextBoundThrowingTask<
	R,
	E1 extends Exception,
	E2 extends Exception,
	E3 extends Exception
> extends ContextBoundClosure<ThrowingTask<R, E1, E2, E3>> implements ThrowingTask<R, E1, E2, E3> {



	public ContextBoundThrowingTask(
		List<TrackableContext<?>> contexts,
		ThrowingTask<R, E1, E2, E3> taskToBind
	) {
		super(contexts, taskToBind);
	}



	@Override
	public R execute() throws E1, E2, E3 {
		return TrackableContext.executeWithinAll(contexts, boundClosure);
	}
}

// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;



/** Executes its wrapped {@link ThrowingTask} within supplied {@code Contexts}. */
public class ContextBoundThrowingTask<
	R,
	E1 extends Exception,
	E2 extends Exception,
	E3 extends Exception,
	E4 extends Exception,
	E5 extends Exception
> extends ContextBoundClosure<
	Throwing5Task<R, E1, E2, E3, E4, E5>
> implements Throwing5Task<R, E1, E2, E3, E4, E5> {



	public ContextBoundThrowingTask(
		List<TrackableContext<?>> contexts,
		Throwing5Task<R, E1, E2, E3, E4, E5> taskToBind
	) {
		super(contexts, taskToBind);
	}



	@Override
	public R execute() throws E1, E2, E3, E4, E5 {
		return TrackableContext.executeWithinAll(contexts, boundClosure);
	}
}

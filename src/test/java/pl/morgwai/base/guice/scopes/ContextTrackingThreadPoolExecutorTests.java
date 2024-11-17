// Copyright 2024 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.DAYS;



public class ContextTrackingThreadPoolExecutorTests extends ContextTrackingExecutorTests {



	static class ContextTrackingThreadPoolExecutor extends ThreadPoolExecutor
			implements ContextTrackingExecutor {

		final Executor rawExecutor = super::execute;
		final ContextBinder ctxBinder;

		ContextTrackingThreadPoolExecutor(ContextBinder ctxBinder, int poolSize) {
			super(poolSize, poolSize, 0, DAYS, new LinkedBlockingQueue<>());
			this.ctxBinder = ctxBinder;
		}

		@Override public Executor getExecutor() {
			return rawExecutor;
		}

		@Override public ContextBinder getContextBinder() {
			return ctxBinder;
		}

		@Override public void execute(Runnable task) {
			ContextTrackingExecutor.super.execute(task);
		}
	}



	@Override
	public void setup() {
		testSubject = new ContextTrackingThreadPoolExecutor(ctxBinder, 1);
		executorToShutdown = (ExecutorService) testSubject;
	}
}

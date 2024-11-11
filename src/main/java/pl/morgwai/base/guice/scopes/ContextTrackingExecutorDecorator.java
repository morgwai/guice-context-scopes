// Copyright 2024 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.concurrent.*;



/**
 * Decorator for {@link ExecutorService} that automatically transfers active
 * {@link TrackableContext Contexts} from {@link ContextTracker Trackers} of the associated
 * {@link ContextBinder}.
 * <p>
 * If {@code ContextTrackingExecutorDecorator}s need to be created before an
 * {@link com.google.inject.Injector}, then an instance of {@link ContextBinder} may be obtained
 * with {@link ScopeModule#newContextBinder()}. Libs derived from
 * {@code guice-context-scopes} usually create a {@code public final ContextBinder} field in their
 * concrete subclasses of {@link ScopeModule}.</p>
 */
public class ContextTrackingExecutorDecorator extends AbstractExecutorService {



	final ExecutorService wrappedExecutor;
	final ContextBinder ctxBinder;



	public ContextTrackingExecutorDecorator(
		ExecutorService executorToWrap,
		ContextBinder ctxBinder
	) {
		this.wrappedExecutor = executorToWrap;
		this.ctxBinder = ctxBinder;
	}



	@Override
	public void execute(Runnable task) {
		wrappedExecutor.execute(ctxBinder.bindToContext(task));
	}



	// only dumb delegations to wrappedExecutor below:

	@Override public void shutdown() {
		wrappedExecutor.shutdown();
	}

	@Override public List<Runnable> shutdownNow() {
		return wrappedExecutor.shutdownNow();
	}

	@Override public boolean isShutdown() {
		return wrappedExecutor.isShutdown();
	}

	@Override public boolean isTerminated() {
		return wrappedExecutor.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return wrappedExecutor.awaitTermination(timeout, unit);
	}
}

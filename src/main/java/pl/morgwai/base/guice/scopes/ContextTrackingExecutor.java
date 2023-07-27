// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;



/**
 * A decorator for an {@link ExecutorService} that automatically updates which thread runs within
 * which {@link TrackableContext context} when executing a task.
 * <p>
 * Note: multiple threads may run within the same context, but the attributes they access must be
 * thread-safe or properly synchronized.</p>
 */
public class ContextTrackingExecutor extends AbstractExecutorService implements ExecutorService {



	final List<ContextTracker<?>> trackers;
	final ExecutorService backingExecutor;



	/** Decorates {@code executorToDecorate}. */
	public ContextTrackingExecutor(
			List<ContextTracker<?>> trackers, ExecutorService executorToDecorate) {
		this.trackers = List.copyOf(trackers);
		this.backingExecutor = executorToDecorate;
	}

	/**
	 * Decorates {@code executorToDecorate} and calls
	 * {@link #decorateRejectedExecutionHandler(ThreadPoolExecutor)
	 * decorateRejectedExecutionHandler(executorToDecorate)}.
	 */
	public ContextTrackingExecutor(
			List<ContextTracker<?>> trackers, ThreadPoolExecutor executorToDecorate) {
		this(trackers, (ExecutorService) executorToDecorate);
		decorateRejectedExecutionHandler(executorToDecorate);
	}

	/**
	 * Decorates {@code executor}'s {@link RejectedExecutionHandler} to unwrap tasks from internal
	 * wrappers before passing them to the original handler.
	 */
	public static void decorateRejectedExecutionHandler(ThreadPoolExecutor executor) {
		final var originalHandler = executor.getRejectedExecutionHandler();
		executor.setRejectedExecutionHandler(
			(rejectedTask, rejectingExecutor) -> originalHandler.rejectedExecution(
				unwrapTask(rejectedTask),
				rejectingExecutor
			)
		);
	}

	/** Removes internal wrappers from {@code task} if needed. */
	public static Runnable unwrapTask(Runnable task) {
		return task instanceof TaskWrapper ? (Runnable) ((TaskWrapper) task).unwrap() : task;
	}



	/** Executes {@code task} within all contexts that were active when this method was called. */
	@Override
	public void execute(Runnable task) {
		final var activeCtxs = getActiveContexts(trackers);
		backingExecutor.execute(new RunnableWrapper(task) {
			@Override public void run() {
				executeWithinAll(activeCtxs, task);
			}
		});
	}



	/**
	 * Retrieves all active contexts from {@code trackers}. The returned list can be then used
	 * as an argument to {@link #executeWithinAll(List, Runnable)} to transfer the contexts after
	 * a switch to another thread.
	 * <p>
	 * Libraries usually bind {@code List<ContextTracker<?>>} to an instance containing all possible
	 * trackers for use as an argument for this method.</p>
	 */
	public static List<TrackableContext<?>> getActiveContexts(List<ContextTracker<?>> trackers) {
		return trackers.stream()
				.map(ContextTracker::getCurrentContext)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}



	/**
	 * Executes {@code task} on the current thread within all {@code contexts}.
	 * Used to transfer active contexts after a switch to another thread.
	 *
	 * @see #getActiveContexts(List)
	 */
	public static void executeWithinAll(List<TrackableContext<?>> contexts, Runnable task) {
		try {
			executeWithinAll(contexts, new CallableRunnable(task));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception ignored) {}  // dead code: result of wrapping task with a Callable
	}

	/**
	 * Executes {@code task} on the current thread within all {@code contexts}.
	 * Used to transfer active contexts after a switch to another thread.
	 *
	 * @see #getActiveContexts(List)
	 */
	public static <T> T executeWithinAll(List<TrackableContext<?>> contexts, Callable<T> task)
			throws Exception {
		switch (contexts.size()) {
			case 1:
				return contexts.get(0).executeWithinSelf(task);
			case 0:
				System.err.println("thread \"" + Thread.currentThread().getName()
						+ "\" is executing task " + task + " outside of any context");
				log.warning("thread \""
						+ Thread.currentThread().getName() + "\" is executing task " + task
						+ " outside of any context"
				);
				return task.call();
			default:
				return executeWithinAll(
					contexts.subList(1, contexts.size()),
					new CallableWrapper<>(task) {
						@Override public T call() throws Exception {
							return contexts.get(0).executeWithinSelf(task);
						}
					}
				);
		}
	}

	static final Logger log = Logger.getLogger(ContextTrackingExecutor.class.getName());



	@Override
	public List<Runnable> shutdownNow() {
		return backingExecutor.shutdownNow().stream()
			.map((task) -> (Runnable) ((TaskWrapper) task).unwrap())
			.collect(Collectors.toList());
	}



	// only dumb delegations to backingExecutor below:

	@Override
	public void shutdown() {
		backingExecutor.shutdown();
	}

	@Override
	public boolean isShutdown() {
		return backingExecutor.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return backingExecutor.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return backingExecutor.awaitTermination(timeout, unit);
	}
}

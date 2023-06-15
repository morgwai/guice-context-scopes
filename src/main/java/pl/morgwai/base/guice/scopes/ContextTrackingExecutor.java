// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * An executor that automatically updates which thread runs within which {@link TrackableContext}
 * when executing a task. By default backed by a fixed size {@link ThreadPoolExecutor}.
 * <p>
 * Instances usually correspond 1-1 with some type of blocking or time consuming operations, such
 * as CPU intensive calculations or blocking network communication with some resource.<br/>
 * In case of network operations, a given threadPool size should usually correspond to the pool size
 * of the connections to the given resource.<br/>
 * In case of CPU intensive operations, it should usually correspond to the number of cores
 * available to the process ({@link Runtime#availableProcessors()}).</p>
 * <p>
 * Instances are usually created at app startup, stored on static vars and/or bound for
 * injection with a specific {@link com.google.inject.name.Names#named(String) name}:</p>
 * <pre>
 * bind(ContextTrackingExecutor.class)
 *     .annotatedWith(Names.named("someOpTypeExecutor"))
 *     .toInstance(...)</pre>
 * <p>
 * and then injected using @{@link com.google.inject.name.Named Named} annotation:</p>
 * <pre>
 * &commat;Named("someOpTypeExecutor")
 * ContextTrackingExecutor someOpTypeExecutor</pre></p>
 * <p>
 * At app shutdown {@link #shutdown()} should be called followed by either
 * {@link #enforceTermination(long, TimeUnit)} or {@link #awaitTermination()} or
 * {@link #awaitTermination(long, TimeUnit)} and {@link #shutdownNow()} in case of a failure.</p>
 * <p>
 * If multiple threads run within the same context, then the attributes they access must be
 * thread-safe or properly synchronized.</p>
 */
public class ContextTrackingExecutor implements Executor {



	final String name;
	public String getName() { return name; }

	final int poolSize;
	public int getPoolSize() { return poolSize; }

	final List<ContextTracker<?>> trackers;

	private final ExecutorService backingExecutor;



	/**
	 * Constructs an instance backed by a new fixed size {@link ThreadPoolExecutor} that uses a
	 * {@link NamedThreadFactory NamedThreadFactory} and an unbound {@link LinkedBlockingQueue}.
	 * <p>
	 * To avoid {@link OutOfMemoryError}s, an external mechanism that limits maximum number of tasks
	 * (such as a load balancer or a frontend proxy) should be used.</p>
	 */
	public ContextTrackingExecutor(String name, int poolSize, List<ContextTracker<?>> trackers) {
		this(name, poolSize, trackers, new LinkedBlockingQueue<>());
	}



	/**
	 * Executes {@code task} within all contexts that were active when this method was called.
	 */
	@Override
	public void execute(Runnable task) {
		final var activeCtxs = getActiveContexts(trackers);
		backingExecutor.execute(() -> executeWithinAll(activeCtxs, task));
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
	 * Executes {@code operation} on the current thread within all {@code contexts}.
	 * Used to transfer active contexts after a switch to another thread.
	 *
	 * @see #getActiveContexts(List)
	 */
	public static void executeWithinAll(List<TrackableContext<?>> contexts, Runnable operation) {
		switch (contexts.size()) {
			case 1:
				contexts.get(0).executeWithinSelf(operation);
				return;
			case 2:
				contexts.get(1).executeWithinSelf(
						() -> contexts.get(0).executeWithinSelf(operation));
				return;
			case 0:
				if (log.isWarnEnabled()) {
					log.warn(Thread.currentThread().getName()
							+ " is executing " + operation + " outside of any context");
				}
				operation.run();
				return;
			default:
				executeWithinAll(
					contexts.subList(1, contexts.size()),
					() -> contexts.get(0).executeWithinSelf(operation)
				);
		}
	}



	/**
	 * Executes {@code operation} on the current thread within all {@code contexts}.
	 * Used to transfer active contexts after a switch to another thread.
	 *
	 * @see #getActiveContexts(List)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T executeWithinAll(List<TrackableContext<?>> contexts, Callable<T> operation)
			throws Exception {
		final Object[] resultHolder = {null};
		final Exception[] exceptionHolder = {null};
		executeWithinAll(contexts, new Runnable() {

			@Override public void run() {
				try {
					resultHolder[0] = operation.call();
				} catch (Exception e) {
					exceptionHolder[0] = e;
				}
			}

			@Override public String toString() { return operation.toString(); }
		});
		if (exceptionHolder[0] != null) throw exceptionHolder[0];
		return (T) resultHolder[0];
	}



	/**
	 * Convenience method to execute a {@link Callable}. If {@link Callable#call() task.call()}
	 * throws an exception, it will be pipelined to
	 * {@link CompletableFuture#handle(BiFunction)  handle(...)} /
	 * {@link CompletableFuture#whenComplete(BiConsumer)  whenComplete(...)} /
	 * {@link CompletableFuture#exceptionally(Function) exceptionally(...)} chained calls.
	 * @see #execute(Runnable)
	 */
	public <T> CompletableFuture<T> execute(Callable<T> task) {
		final var result = new CompletableFuture<T>();
		execute(
			() -> {
				try {
					result.complete(task.call());
				} catch (Exception e) {
					result.completeExceptionally(e);
				}
			}
		);
		return result;
	}



	@Override
	public String toString() {
		return "ContextTrackingExecutor { name = \"" + name + "\" }";
	}



	// in addition to a RejectedExecutionException, logs a warning if the executor is overloaded
	final RejectedExecutionHandler rejectionHandler = (task, executor) -> {
		if (executor.isShutdown()) {
			throw new RejectedExecutionException(getName() + " rejected a task due to a shutdown");
		} else {
			if (log.isWarnEnabled()) log.warn("executor " + getName() + " is overloaded");
			throw new RejectedExecutionException(getName() + " rejected a task due to an overload");
		}
	};




	/**
	 * Constructs an instance backed by a new fixed size {@link ThreadPoolExecutor} that uses a
	 * {@link NamedThreadFactory NamedThreadFactory}.
	 * <p>
	 * {@link #execute(Runnable)} throws a {@link RejectedExecutionException} if {@code workQueue}
	 * is full. It should usually be handled by informing the client that the service has
	 * temporarily exceeded its capacity (for example a gRPC can send status {@code UNAVAILABLE(14)}
	 * and a servlet can send status {@code 503 Service Unavailable}).</p>
	 */
	public ContextTrackingExecutor(
		String name,
		int poolSize,
		List<ContextTracker<?>> trackers,
		BlockingQueue<Runnable> workQueue
	) {
		this(
			name,
			poolSize,
			trackers,
			workQueue,
			new NamedThreadFactory(name)
		);
	}



	/**
	 * Constructs an instance backed by a new fixed size {@link ThreadPoolExecutor}.
	 * <p>
	 * {@link #execute(Runnable)} throws a {@link RejectedExecutionException} if {@code workQueue}
	 * is full. It should usually be handled by informing the client that the service has
	 * temporarily exceeded its capacity (for example a gRPC can send status {@code UNAVAILABLE(14)}
	 * and a servlet can send status {@code 503 Service Unavailable}).</p>
	 */
	public ContextTrackingExecutor(
		String name,
		int poolSize,
		List<ContextTracker<?>> trackers,
		BlockingQueue<Runnable> workQueue,
		ThreadFactory threadFactory
	) {
		this(
			name,
			poolSize,
			trackers,
			workQueue,
			threadFactory,
			null
		);
	}



	/**
	 * Constructs an instance backed by {@code backingExecutor}.
	 * @param poolSize informative only: to be returned by {@link #getPoolSize()}.
	 */
	public ContextTrackingExecutor(
		String name,
		int poolSize,
		List<ContextTracker<?>> trackers,
		ExecutorService backingExecutor
	) {
		this(
			name,
			poolSize,
			trackers,
			null,
			null,
			backingExecutor
		);
	}



	private ContextTrackingExecutor(
		String name,
		int poolSize,
		List<ContextTracker<?>> trackers,
		BlockingQueue<Runnable> workQueue,
		ThreadFactory threadFactory,
		ExecutorService backingExecutor
	) {
		this.name = name;
		this.poolSize = poolSize;
		this.trackers = List.copyOf(trackers);
		this.backingExecutor = backingExecutor != null
				? backingExecutor
				: new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.SECONDS,
						workQueue, threadFactory, rejectionHandler);
	}



	/**
	 * Calls {@link ExecutorService#shutdown() backingExecutor.shutdown()}.
	 */
	public void shutdown() {
		backingExecutor.shutdown();
	}



	/**
	 * Calls {@link ExecutorService#isShutdown() backingExecutor.isShutdown()}.
	 */
	public boolean isShutdown() {
		return backingExecutor.isShutdown();
	}



	/**
	 * Calls {@link ExecutorService#isTerminated() backingExecutor.isTerminated()}.
	 */
	public boolean isTerminated() {
		return backingExecutor.isTerminated();
	}



	/**
	 * {@link #awaitTermination(long, TimeUnit) Awaits} up to {@code timeoutMillis} for termination
	 * and if executor fails to do so either due to timeout or interrupt {@link #shutdownNow()} is
	 * called.<br/>
	 * Logs outcome to {@link Logger} named after this class.
	 * <p>
	 * Should be called at app shutdown.</p>
	 * @return {@link Optional#empty() empty} if the executor was shutdown cleanly, list of tasks
	 *     returned by {@code backingExecutor.shutdownNow()} otherwise.
	 * @see ExecutorService#awaitTermination(long, TimeUnit)
	 * @see ExecutorService#shutdownNow()
	 */
	public Optional<List<Runnable>> enforceTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		try {
			if (awaitTermination(timeout, unit)) {
				if (log.isInfoEnabled()) log.info("executor " + name + " shutdown completed");
				return Optional.empty();
			} else {
				if (log.isWarnEnabled()) log.warn("executor " + name + " hasn't shutdown cleanly");
				return Optional.of(backingExecutor.shutdownNow());
			}
		} catch (InterruptedException e) {
			if (log.isWarnEnabled()) log.warn("shutdown of executor " + name + " was interrupted");
			backingExecutor.shutdownNow();
			throw e;
		}
	}



	/**
	 * Calls {@link ExecutorService#awaitTermination(long, TimeUnit)
	 * backingExecutor.awaitTermination(...)}.
	 * @see #enforceTermination(long, TimeUnit)
	 */
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return backingExecutor.awaitTermination(timeout, unit);
	}



	/**
	 * Keeps calling {@link ExecutorService#awaitTermination(long, TimeUnit)
	 * backingExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)} until it returns
	 * {@code true}.
	 * @see #enforceTermination(long, TimeUnit)
	 * @see #awaitTermination(long, TimeUnit)
	 */
	public void awaitTermination() throws InterruptedException {
		while ( !backingExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS));
	}



	/**
	 * Calls {@link ExecutorService#shutdown() backingExecutor.shutdownNow()}.
	 * @see #enforceTermination(long, TimeUnit)
	 */
	public List<Runnable> shutdownNow() {
		return backingExecutor.shutdownNow();
	}



	/**
	 * A thread factory that names new threads based on its own name. Each instance has an
	 * associated thread group (named after itself), which newly created threads will belong to.
	 * All such instance associated thread groups have a common parent (named
	 * {@value #PARENT_THREAD_GROUP_NAME}), which in turn is a child of the system default thread
	 * group (obtained from system security manager under normal circumstances).
	 */
	public static class NamedThreadFactory implements ThreadFactory {

		static final ThreadGroup contextTrackingExecutors;

		final AtomicInteger threadNumber = new AtomicInteger(1);
		final ThreadGroup threadGroup;
		final String namePrefix;



		/**
		 * Constructs a factory that will assign {@link Thread#NORM_PRIORITY} to created threads.
		 */
		public NamedThreadFactory(String name) {
			this(name, Thread.NORM_PRIORITY);
		}



		/**
		 * Constructs a factory that will assign {@code priority} to created threads.
		 */
		public NamedThreadFactory(String name, int priority) {
			threadGroup = new ThreadGroup(contextTrackingExecutors, name);
			threadGroup.setMaxPriority(priority);
			namePrefix = name + "-thread-";
		}



		/**
		 * Creates a new thread named {@code <thisFactoryName>-thread-<sequenceNumber>}.
		 */
		@Override
		public Thread newThread(Runnable task) {
			final var thread =
					new Thread(threadGroup, task, namePrefix + threadNumber.getAndIncrement());
			thread.setPriority(threadGroup.getMaxPriority());
			return thread;
		}



		/**
		 * Name of the parent of all thread groups associated with {@link ContextTrackingExecutor}
		 * instances.
		 */
		public static final String PARENT_THREAD_GROUP_NAME = "ContextTrackingExecutors";

		static {
			final var securityManager = System.getSecurityManager();
			final var parentThreadGroup = securityManager != null
					? securityManager.getThreadGroup()
					: Thread.currentThread().getThreadGroup();
			contextTrackingExecutors =
					new ThreadGroup(parentThreadGroup, PARENT_THREAD_GROUP_NAME);
			contextTrackingExecutors.setDaemon(false);
			contextTrackingExecutors.setMaxPriority(Thread.MAX_PRIORITY);
		}
	}



	protected static final Logger log =
			LoggerFactory.getLogger(ContextTrackingExecutor.class.getName());
}

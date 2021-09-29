// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * An executor that automatically updates which thread runs within which {@link ServerSideContext}
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
 *bind(ContextTrackingExecutor.class)
 *    .annotatedWith(Names.named("someOpTypeExecutor"))
 *    .toInstance(...)</pre>
 * <p>
 * and then injected using @{@link com.google.inject.name.Named Named} annotation:</p>
 * <pre>
 *&commat;Named("someOpTypeExecutor")
 *ContextTrackingExecutor someOpTypeExecutor</pre></p>
 * <p>
 * At app shutdown {@link #tryShutdownGracefully(int)} should be called on every instance.</p>
 * <p>
 * If multiple threads run within the same context, then the attributes they access must be
 * thread-safe or properly synchronized.</p>
 */
public class ContextTrackingExecutor implements Executor {



	final ContextTracker<?>[] trackers;

	final ExecutorService backingExecutor;

	final int poolSize;
	public int getPoolSize() { return poolSize; }

	final String name;
	public String getName() { return name; }



	/**
	 * Constructs an instance backed by a new fixed size {@link ThreadPoolExecutor} that uses a
	 * {@link NamedThreadFactory} and an unbound {@link LinkedBlockingQueue}.
	 * <p>
	 * To avoid {@link OutOfMemoryError}s, an external mechanism that limits maximum number of tasks
	 * (such as a load balancer or a frontend proxy) should be used.</p>
	 */
	public ContextTrackingExecutor(String name, int poolSize, ContextTracker<?>... trackers) {
		this(name, poolSize, new LinkedBlockingQueue<>(), trackers);
	}



	/**
	 * Convenience method to execute a {@link Callable}.
	 */
	public <T> CompletableFuture<T> execute(Callable<T> task) {
		return CompletableFuture.supplyAsync(
			() -> {
				try {
					return task.call();
				} catch (CompletionException e) {
					throw e;
				} catch (Exception e) {
					throw new CompletionException(e);
				}
			},
			this
		);
	}



	@Override
	public void execute(Runnable task) {
		final var activeCtxs = getActiveContexts(trackers);
		backingExecutor.execute(() -> executeWithinAll(activeCtxs, task));
	}



	/**
	 * Retrieves all active contexts from {@code trackers}. The returned list can be then used
	 * as an argument to {@link #executeWithinAll(List, Callable)} to transfer the contexts after
	 * a switch to another thread.
	 * <p>
	 * Libraries usually bind {@code ContextTracker<?>[]} to an instance containing all possible
	 * trackers for use as an argument for this method.</p>
	 */
	public static List<ServerSideContext<?>> getActiveContexts(ContextTracker<?>... trackers) {
		return Arrays.stream(trackers)
				.map((tracker) -> tracker.getCurrentContext())
				.filter((ctx) -> ctx != null)
				.collect(Collectors.toList());
	}



	/**
	 * Executes {@code operation} synchronously (on the current thread) within all contexts supplied
	 * via {@code ctxs}. Used to transfer active contexts after a switch to another thread.
	 *
	 * @see #getActiveContexts(ContextTracker...)
	 */
	public static void executeWithinAll(List<ServerSideContext<?>> ctxs, Runnable operation) {
		switch (ctxs.size()) {
			case 1:
				ctxs.get(0).executeWithinSelf(operation);
				return;
			case 2:
				ctxs.get(1).executeWithinSelf(() -> ctxs.get(0).executeWithinSelf(operation));
				return;
			case 0:
				log.warn(Thread.currentThread().getName() + " is running outside of any context");
				operation.run();
				return;
			default:
				executeWithinAll(
					ctxs.subList(1, ctxs.size()),
					() -> ctxs.get(0).executeWithinSelf(operation)
				);
		}
	}



	/**
	 * Executes {@code operation} synchronously (on the current thread) within all contexts supplied
	 * via {@code ctxs}. Used to transfer active contexts after a switch to another thread.
	 *
	 * @see #getActiveContexts(ContextTracker...)
	 */
	public static <T> T executeWithinAll(List<ServerSideContext<?>> ctxs, Callable<T> operation)
			throws Exception {
		switch (ctxs.size()) {
			case 1:
				return ctxs.get(0).executeWithinSelf(operation);
			case 2:
				return ctxs.get(1).executeWithinSelf(
						() -> ctxs.get(0).executeWithinSelf(operation));
			case 0:
				log.warn(Thread.currentThread().getName() + " is running outside of any context");
				return operation.call();
			default:
				return executeWithinAll(
					ctxs.subList(1, ctxs.size()),
					() -> ctxs.get(0).executeWithinSelf(operation)
				);
		}
	}



	/**
	 * Constructs an instance backed by a new fixed size {@link ThreadPoolExecutor} that uses a
	 * {@link NamedThreadFactory}.
	 * <p>
	 * {@link #execute(Runnable)} throws a {@link RejectedExecutionException} if {@code workQueue}
	 * is full. It should usually be handled by informing the client that the service has
	 * temporarily exceeded its capacity (for example a gRPC can send status {@code UNAVAILABLE(14)}
	 * and a servlet can send status {@code 503 Service Unavailable}).</p>
	 */
	public ContextTrackingExecutor(
			String name,
			int poolSize,
			BlockingQueue<Runnable> workQueue,
			ContextTracker<?>... trackers) {
		this.name = name;
		this.poolSize = poolSize;
		this.trackers = trackers;
		backingExecutor = new ThreadPoolExecutor(
				poolSize, poolSize, 0l, TimeUnit.SECONDS,
				workQueue, new NamedThreadFactory(name), rejectionHandler);
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
			BlockingQueue<Runnable> workQueue,
			ThreadFactory threadFactory,
			ContextTracker<?>... trackers) {
		this.name = name;
		this.poolSize = poolSize;
		this.trackers = trackers;
		backingExecutor = new ThreadPoolExecutor(
				poolSize, poolSize, 0l, TimeUnit.SECONDS,
				workQueue, threadFactory, rejectionHandler);
	}



	/**
	 * Constructs an instance backed by {@code backingExecutor}.
	 * {@code poolSize} is informative only, to be returned by {@link #getPoolSize()}.
	 */
	public ContextTrackingExecutor(
			String name,
			ExecutorService backingExecutor,
			int poolSize,
			ContextTracker<?>... trackers) {
		this.name = name;
		this.backingExecutor = backingExecutor;
		this.poolSize = poolSize;
		this.trackers = trackers;
	}



	/**
	 * Calls {@code backingExecutor.shutdown()} and awaits up to <code>timeoutSeconds</code> for
	 * termination. If it fails, calls {@code backingExecutor.shutdownNow()}.<br/>
	 * Logs outcome to {@link Logger} named after this class.
	 * <p>
	 * Should be called at app shutdown.</p>
	 * @return <code>null</code> if the executor was shutdown cleanly, list of tasks returned by
	 *     {@code backingExecutor.shutdownNow()} otherwise.
	 */
	public List<Runnable> tryShutdownGracefully(int timeoutSeconds) {
		backingExecutor.shutdown();
		try {
			backingExecutor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {}
		if ( ! backingExecutor.isTerminated()) {
			log.warn("executor " + name + " hasn't shutdown cleanly");
			return backingExecutor.shutdownNow();
		} else {
			log.info("executor " + name + " shutdown completed");
			return null;
		}
	}



	// in addition to a RejectedExecutionException, logs a warning if the executor is overloaded
	final RejectedExecutionHandler rejectionHandler = (task, executor) -> {
		if (executor.isShutdown()) {
			throw new RejectedExecutionException(getName() + " rejcted a task due to shutdown");
		} else {
			log.warn("executor " + getName() + " is overloaded");
			throw new RejectedExecutionException(getName() + " rejcted a task due to overload");
		}
	};



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



		NamedThreadFactory(String name) {
			threadGroup = new ThreadGroup(contextTrackingExecutors, name);
			namePrefix = name + "-thread-";
		}



		/**
		 * Creates a new thread named {@code <thisFactoryName>-thread-<sequenceNumber>}.
		 */
		@Override
		public Thread newThread(Runnable task) {
			return new Thread(threadGroup, task, namePrefix + threadNumber.getAndIncrement());
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
		}
	}



	protected static final Logger log =
			LoggerFactory.getLogger(ContextTrackingExecutor.class.getName());
}

// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * A {@link ThreadPoolExecutor} that upon task execution automatically updates which thread runs
 * within which {@link ServerSideContext} using supplied {@link #trackers}.
 * <p>
 * Instances usually correspond 1-1 with some type of blocking or time consuming operations, such
 * as CPU/GPU intensive calculations or blocking network communication with some resource.<br/>
 * In case of network operations, a given threadPool size should usually correspond to the pool size
 * of the connections to the given resource.<br/>
 * In case of CPU/GPU intensive operations, it should usually correspond to the number of given
 * cores available to the process.</p>
 * <p>
 * Instances are usually created at app startup, stored on static vars and/or configured for
 * injection using<pre>
 * bind(ContextTrackingExecutor.class)
 *    .annotatedWith(Names.named("someOpTypeExecutor"))
 *    .toInstance(...)</pre>
 * and injected with
 * <pre>@Named("someOpTypeExecutor") ContextTrackingExecutor someOpTypeExecutor</pre></p>
 * <p>
 * If multiple threads run within the same context (for example by using
 * {@link #invokeAll(Collection)}), then the attributes they access must be thread-safe or properly
 * synchronized.</p>
 */
public class ContextTrackingExecutor extends ThreadPoolExecutor {



	final ContextTracker<?>[] trackers;

	final String name;
	public String getName() { return name; }



	public ContextTrackingExecutor(String name, int poolSize, ContextTracker<?>... trackers) {
		super(poolSize, poolSize, 0l, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
				new NamedThreadFactory(name));
		this.name = name;
		this.trackers = trackers;
	}



	@Override
	public void execute(Runnable task) {
		final List<ServerSideContext<?>> ctxs = getCurrentContexts();
		super.execute(() -> executeWithinAll(ctxs, task));
	}



	private List<ServerSideContext<?>> getCurrentContexts() {
		final var ctxs = new LinkedList<ServerSideContext<?>>();
		for (int i = 0; i < trackers.length; i++) {
			final var ctx = trackers[i].getCurrentContext();
			if (ctx != null) ctxs.add(ctx);
		}
		return ctxs;
	}



	private static void executeWithinAll(List<ServerSideContext<?>> ctxs, Runnable operation) {
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



	private static <T> T executeWithinAll(List<ServerSideContext<?>> ctxs, Callable<T> operation)
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



	@Override
	public <T> Future<T> submit(Callable<T> task) {
		final List<ServerSideContext<?>> ctxs = getCurrentContexts();
		return super.submit(() -> executeWithinAll(ctxs, task));
	}



	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		final List<ServerSideContext<?>> ctxs = getCurrentContexts();
		return super.submit(
			() -> executeWithinAll(ctxs, task),
			result
		);
	}



	@Override
	public Future<?> submit(Runnable task) {
		final List<ServerSideContext<?>> ctxs = getCurrentContexts();
		return super.submit(() -> executeWithinAll(ctxs, task));
	}



	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		return super.invokeAll(wrapTasks(tasks));
	}



	private <T> List<Callable<T>> wrapTasks(Collection<? extends Callable<T>> tasks) {
		final List<ServerSideContext<?>> ctxs = getCurrentContexts();
		final var wrappedTasks = new ArrayList<Callable<T>>(tasks.size() + 1);
		for(final var task: tasks) {
			wrappedTasks.add(() -> executeWithinAll(ctxs, task));
		}
		return wrappedTasks;
	}



	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
			TimeUnit unit) throws InterruptedException {
		return super.invokeAll(wrapTasks(tasks), timeout, unit);
	}



	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		return super.invokeAny(wrapTasks(tasks));
	}



	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return super.invokeAny(wrapTasks(tasks), timeout, unit);
	}



	public ContextTrackingExecutor(
			String name,
			int poolSize,
			BlockingQueue<Runnable> workQueue,
			ContextTracker<?>... trackers) {
		super(poolSize, poolSize, 0l, TimeUnit.SECONDS, workQueue);
		this.name = name;
		this.trackers = trackers;
	}



	public ContextTrackingExecutor(
			String name,
			int corePoolSize,
			int maximumPoolSize,
			long keepAliveTime,
			TimeUnit unit,
			BlockingQueue<Runnable> workQueue,
			ThreadFactory threadFactory,
			RejectedExecutionHandler handler,
			ContextTracker<?>... trackers) {
		super(
				corePoolSize,
				maximumPoolSize,
				keepAliveTime,
				unit,
				workQueue,
				threadFactory,
				handler);
		this.name = name;
		this.trackers = trackers;
	}



	/**
	 * Calls {@link #shutdown()} and waits <code>timeoutSeconds</code> for termination. If it fails,
	 * calls {@link #shutdownNow()}.
	 * Logs outcome to {@link Logger} named after this class.
	 * @return <code>null</code> if the executor was shutdown cleanly, list of tasks returned by
	 *     {@link #shutdownNow()} otherwise.
	 */
	public List<Runnable> tryShutdownGracefully(long timeoutSeconds) {
		shutdown();
		try {
			awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {}
		if ( ! isTerminated()) {
			final int activeCount = getActiveCount();
			final List<Runnable> unstartedTasks = shutdownNow();
			log.warn(activeCount + " active and " + unstartedTasks.size()
					+ " unstarted tasks are still remaining in executor " + name);
			return unstartedTasks;
		} else {
			log.info("executor " + name + " shutdown completed");
			return null;
		}
	}



	static class NamedThreadFactory implements ThreadFactory {

		static final ThreadGroup contextTrackingExecutors;

		final ThreadGroup threadGroup;
		final AtomicInteger threadNumber;
		final String namePrefix;



		static {
			final var securityManager = System.getSecurityManager();
			final var parentThreadGroup = securityManager != null
					? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
			contextTrackingExecutors =
					new ThreadGroup(parentThreadGroup, "ContextTrackingExecutors");
			contextTrackingExecutors.setDaemon(false);
		}



		NamedThreadFactory(String name) {
			threadGroup = new ThreadGroup(contextTrackingExecutors, name);
			threadNumber = new AtomicInteger(1);
			namePrefix = name + "-thread-";
		}



		public Thread newThread(Runnable task) {
			return new Thread(threadGroup, task, namePrefix + threadNumber.getAndIncrement());
		}
	}



	static final Logger log = LoggerFactory.getLogger(ContextTrackingExecutor.class.getName());
}

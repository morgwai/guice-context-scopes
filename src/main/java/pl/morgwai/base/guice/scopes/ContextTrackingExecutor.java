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
import java.util.logging.Logger;



/**
 * A <code>ThreadPoolExecutor</code> that upon dispatching automatically updates which thread runs
 * within which {@link ServerSideContext} using supplied {@link #trackers}.<br/>
 * <br/>
 * Usually instances correspond 1-1 with some type of blocking or time consuming operations, such
 * as CPU/GPU intensive calculations or blocking network communication.<br/>
 * In case of network operations, a given threadPool size should usually correspond to the pool size
 * of the connections to a given resource.<br/>
 * In case CPU/GPU intensive operations, it should usually correspond to the number of given cores
 * available to the process.<br/>
 * <br/>
 * Instances are usually created at app startup, stored on static vars and/or configured for
 * injection using <code>toInstance(...)</code> with <code>@Named</code> annotation.<br/>
 * <br/>
 * Note: as long as accessed scoped objects are thread-safe, it is generally ok to handle a single
 * context in multiple threads (for example by using one of
 * <code>invokeAll(...)</code> / <code>invokeAny(...)</code> methods, or dispatching and then
 * continuing work also on the original thread).
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
		super.execute(() -> runWithinAll(ctxs, task));
	}



	private List<ServerSideContext<?>> getCurrentContexts() {
		final var ctxs = new LinkedList<ServerSideContext<?>>();
		for (int i = 0; i < trackers.length; i++) {
			final var ctx = trackers[i].getCurrentContext();
			if (ctx != null) ctxs.add(ctx);
		}
		return ctxs;
	}



	private static void runWithinAll(List<ServerSideContext<?>> ctxs, Runnable operation) {
		if (ctxs.size() == 1) {
			ctxs.get(0).runWithinSelf(operation);
			return;
		}
		if (ctxs.size() == 2) {
			ctxs.get(1).runWithinSelf(() -> ctxs.get(0).runWithinSelf(operation));
			return;
		}
		runWithinAll(
			ctxs.subList(1, ctxs.size()),
			() -> ctxs.get(0).runWithinSelf(operation)
		);
	}



	private static <T> T callWithinAll(List<ServerSideContext<?>> ctxs, Callable<T> operation)
			throws Exception {
		if (ctxs.size() == 1) return ctxs.get(0).callWithinSelf(operation);
		if (ctxs.size() == 2) {
			return ctxs.get(1).callWithinSelf(() -> ctxs.get(0).callWithinSelf(operation));
		}
		return callWithinAll(
			ctxs.subList(1, ctxs.size()),
			() -> ctxs.get(0).callWithinSelf(operation)
		);
	}



	@Override
	public <T> Future<T> submit(Callable<T> task) {
		final List<ServerSideContext<?>> ctxs = getCurrentContexts();
		return super.submit(() -> callWithinAll(ctxs, task));
	}



	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		final List<ServerSideContext<?>> ctxs = getCurrentContexts();
		return super.submit(
			() -> runWithinAll(ctxs, task),
			result
		);
	}



	@Override
	public Future<?> submit(Runnable task) {
		final List<ServerSideContext<?>> ctxs = getCurrentContexts();
		return super.submit(() -> runWithinAll(ctxs, task));
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
			wrappedTasks.add(() -> callWithinAll(ctxs, task));
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
	 * @return <code>null</code> if the executor was shutdown cleanly, list of remaining tasks
	 *     otherwise.
	 */
	public List<Runnable> tryShutdownGracefully(long timeoutSeconds) {
		shutdown();
		try {
			awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {}
		if ( ! isTerminated()) {
			List<Runnable> remianingTasks = shutdownNow();
			log.warning(remianingTasks.size() + " tasks still remaining in executor " + name);
			return remianingTasks;
		} else {
			log.info("executor" + name + " shutdown completed");
			return null;
		}
	}

	static final Logger log = Logger.getLogger(ContextTrackingExecutor.class.getName());



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
}

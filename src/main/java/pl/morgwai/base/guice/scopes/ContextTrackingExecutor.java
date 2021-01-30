/*
 * Copyright (c) Piotr Morgwai Kotarbinski
 */
package pl.morgwai.base.guice.scopes;

import java.util.ArrayList;
import java.util.Collection;
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



/**
 * A <code>ThreadPoolExecutor</code> that upon dispatching automatically updates which thread
 * handles which {@link ServerCallContext} using supplied {@link #trackers}. As context
 * attributes are often not thread-safe, to avoid concurrency errors, dispatching should preferably
 * be the last instructions of the processing by the previous thread.<br/>
 * Usually instances correspond 1-1 with some type of blocking or time consuming operations, such
 * as CPU/GPU intensive calculations or blocking network communication with external resources.<br/>
 * In case of network operations, a given threadPool size should usually correspond to the pool size
 * of the connections to a given resource.
 * In case CPU/GPU intensive operations, it should usually correspond to the number of given cores
 * available to the process.<br/>
 * Instances are usually created at app startup, stored on static vars and configured for injection
 * using <code>@Named</code> annotation.
 */
public class ContextTrackingExecutor extends ThreadPoolExecutor {



	String name;
	public String getName() { return name; }

	@SuppressWarnings("rawtypes")
	ContextTracker[] trackers;



	public ContextTrackingExecutor(
			String name,
			int poolSize,
			@SuppressWarnings("rawtypes") ContextTracker... trackers) {
		super(poolSize, poolSize, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
				new NamedThreadFactory(name));
		this.name = name;
		this.trackers = trackers;
	}



	public ContextTrackingExecutor(
			String name,
			int poolSize,
			BlockingQueue<Runnable> workQueue,
			ThreadFactory threadFactory,
			RejectedExecutionHandler handler,
			@SuppressWarnings("rawtypes") ContextTracker... trackers) {
		super(poolSize, poolSize, 0, TimeUnit.SECONDS, workQueue, threadFactory, handler);
		this.name = name;
		this.trackers = trackers;
	}



	private void runWithinAll(ServerCallContext<?>[] ctxs, Runnable op) {
		runWithinAll(0, ctxs, op);
	}

	@SuppressWarnings("unchecked")
	private void runWithinAll(int i, ServerCallContext<?>[] ctxs, Runnable op) {
		if (i == ctxs.length) {
			op.run();
			return;
		}
		runWithinAll(
			i + 1,
			ctxs,
			() -> trackers[i].runWithin(ctxs[i], op)
		);
	}



	public <T> T callWithinAll(ServerCallContext<?>[] ctxs, Callable<T> op) throws Exception {
		return callWithinAll(0, ctxs, op);
	}

	@SuppressWarnings("unchecked")
	public <T> T callWithinAll(int i, ServerCallContext<?>[] ctxs, Callable<T> op) throws Exception
	{
		if (i == ctxs.length) {
			return op.call();
		}
		return (T) callWithinAll(
			i + 1,
			ctxs,
			() -> trackers[i].callWithin(ctxs[i], op)
		);
	}



	private ServerCallContext<?>[] getCurrentContexts() {
		ServerCallContext<?>[] ctxs = new ServerCallContext[trackers.length];
		for (int i = 0; i < trackers.length; i++) {
			ctxs[i] = trackers[i].getCurrentContext();
		}
		return ctxs;
	}



	@Override
	public void execute(Runnable task) {
		ServerCallContext<?>[] ctxs = getCurrentContexts();
		super.execute(() -> runWithinAll(ctxs, task));
	}



	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ServerCallContext<?>[] ctxs = getCurrentContexts();
		return super.submit(() -> callWithinAll(ctxs, task));
	}



	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		ServerCallContext<?>[] ctxs = getCurrentContexts();
		return super.submit(
			() -> runWithinAll(ctxs, task),
			result
		);
	}



	@Override
	public Future<?> submit(Runnable task) {
		ServerCallContext<?>[] ctxs = getCurrentContexts();
		return super.submit(() -> runWithinAll(ctxs, task));
	}



	/**
	 * @deprecated Accessing the same context from multiple threads is likely to cause concurrency
	 * disasters as context attributes are often not thread-safe.
	 */
	@Deprecated
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		ServerCallContext<?>[] ctxs = getCurrentContexts();
		List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size() + 1);
		for(Callable<T> task: tasks) {
			wrappedTasks.add(() -> callWithinAll(ctxs, task));
		}
		return super.invokeAll(wrappedTasks);
	}



	/**
	 * @deprecated Accessing the same context from multiple threads is likely to cause concurrency
	 * disasters as context attributes are often not thread-safe.
	 */
	@Deprecated
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
			TimeUnit unit) throws InterruptedException {
		ServerCallContext<?>[] ctxs = getCurrentContexts();
		List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size() + 1);
		for(Callable<T> task: tasks) {
			wrappedTasks.add(() -> callWithinAll(ctxs, task));
		}
		return super.invokeAll(wrappedTasks, timeout, unit);
	}



	/**
	 * @deprecated Accessing the same context from multiple threads is likely to cause concurrency
	 * disasters as context attributes are often not thread-safe.
	 */
	@Deprecated
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		ServerCallContext<?>[] ctxs = getCurrentContexts();
		List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size() + 1);
		for(Callable<T> task: tasks) {
			wrappedTasks.add(() -> callWithinAll(ctxs, task));
		}
		return super.invokeAny(wrappedTasks);
	}



	/**
	 * @deprecated Accessing the same context from multiple threads is likely to cause concurrency
	 * disasters as context attributes are often not thread-safe.
	 */
	@Deprecated
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		ServerCallContext<?>[] ctxs = getCurrentContexts();
		List<Callable<T>> wrappedTasks = new ArrayList<>(tasks.size() + 1);
		for(Callable<T> task: tasks) {
			wrappedTasks.add(() -> callWithinAll(ctxs, task));
		}
		return super.invokeAny(wrappedTasks, timeout, unit);
	}



	static class NamedThreadFactory implements ThreadFactory {



		static final ThreadGroup contextTrackingExecutors;



		static {
			SecurityManager securityManager = System.getSecurityManager();
			ThreadGroup parent = securityManager != null
					? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
			contextTrackingExecutors = new ThreadGroup(parent, "ContextTrackingExecutors");
			contextTrackingExecutors.setDaemon(false);
		}



		final ThreadGroup threadGroup;
		final AtomicInteger threadNumber;
		final String namePrefix;



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

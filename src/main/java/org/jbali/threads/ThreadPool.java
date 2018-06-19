package org.jbali.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Static container for a single <code>Executors.newCachedThreadPool()</code>
 * which can be used everywhere, to minimize the number of independent executors
 * and thus threads.
 * Also offers shorthands for the most often used methods of the executor.
 */
public final class ThreadPool {
	
	private ThreadPool() {}

	// TODO require explicit init.
	// TODO delegate executor.
	public static ExecutorService executor = Executors.newCachedThreadPool();

	/**
	 * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
	 */
	public static void execute(Runnable command) {
		executor.execute(command);
	}
	
	public static void execute(Runnable command, String threadName) {
		executor.execute(namedRunnable(command, threadName));
	}

	public static <T> Future<T> submit(Callable<T> task) {
		return executor.submit(task);
	}

	/**
	 * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
	 */
	public static Future<?> submit(Runnable task) {
		return executor.submit(task);
	}
	
	public static Future<?> submit(Runnable task, String threadName) {
		return executor.submit(namedRunnable(task, threadName));
	}

	public static void shutdown() {
		executor.shutdown();
	}

	private static Runnable namedRunnable(Runnable r, String name) {
		return () -> {
			final Thread ct = Thread.currentThread();
			final String orgName = ct.getName();
			ct.setName(orgName + ": " + name);
			try {
				r.run();
			} finally {
				ct.setName(orgName);
			}
		};
	}
	
}

package org.jbali.threads;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
	public static ExecutorService executor = new ThreadPoolExecutor(
			Runtime.getRuntime().availableProcessors(), 2147483647, 10L, TimeUnit.SECONDS,
			new SynchronousQueue<>(),
			new ThreadFactory() {
				private final AtomicInteger threadNumber = new AtomicInteger(1);
				@Override
				public Thread newThread(@NotNull Runnable runnable) {
					Thread t = new Thread(runnable, ThreadPool.class.getName() + "-" + this.threadNumber.getAndIncrement());
					if (t.isDaemon()) {
						t.setDaemon(false);
					}
					if (t.getPriority() != 5) {
						t.setPriority(5);
					}
					return t;
				}
			}
	);

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

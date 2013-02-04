package org.jbali.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Static wrapper around a single <code>Executors.newCachedThreadPool()</code>
 * which can be used everywhere, to minimize the number of independent executors
 * and thus threads.
 * 
 * @author Bart van Heukelom
 */
public final class ThreadPool {
	
	private ThreadPool() {}
	
	private static ExecutorService exec = Executors.newCachedThreadPool();

	/**
	 * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
	 */
	public static void execute(Runnable command) {
		exec.execute(command);
	}
	
	public static void execute(Runnable command, String threadName) {
		exec.execute(new NamedRunnable(command, threadName));
	}

	public static <T> Future<T> submit(Callable<T> task) {
		return exec.submit(task);
	}

	/**
	 * @see java.util.concurrent.ExecutorService#submit(java.lang.Runnable)
	 */
	public static Future<?> submit(Runnable task) {
		return exec.submit(task);
	}
	
	public static Future<?> submit(Runnable task, String threadName) {
		return exec.submit(new NamedRunnable(task, threadName));
	}

	public static void shutdown() {
		exec.shutdown();
	}
	
	private static class NamedRunnable implements Runnable {
		
		private final Runnable command;
		private final String threadName;
		
		public NamedRunnable(Runnable command, String threadName) {
			this.command = command;
			this.threadName = threadName;
		}
		
		@Override public void run() {
			final Thread ct = Thread.currentThread();
			final String orgName = ct.getName();
			ct.setName(orgName + ": " + threadName);
			try {
				command.run();
			} finally {
				ct.setName(orgName);
			}
		}
		
	}
	
}

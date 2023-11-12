package org.jbali.threads

import org.jbali.util.OneTimeFlag
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * Contains various convenience methods for creating [ThreadFactory] instances.
 *
 * This is a bad name and perhaps even a bad design, but I like the meme.
 */
object ThreadFactoryFactory {
    
    /**
     * Create a thread factory which calls the given factory function the first time it is asked to create a thread,
     * and throws [IllegalStateException] for any subsequent calls.
     */
    fun once(factory: (Runnable) -> Thread): ThreadFactory =
        object : ThreadFactory {
            private val used = OneTimeFlag()
            override fun newThread(r: Runnable): Thread  {
                used.flag()
                return factory(r)
            }
        }
    
    /**
     * Create a thread factory which returns new thread with the given properties the first time it is used
     * and throws [IllegalStateException] for any subsequent uses.
     */
    fun once(
        name: String? = null,
        daemon: Boolean? = null,
        priority: Int? = null,
        uncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null,
    ): ThreadFactory = once { r ->
        Thread(r).apply {
            name?.let { this.name = it }
            daemon?.let { this.isDaemon = it }
            priority?.let { this.priority = it }
            uncaughtExceptionHandler?.let { this.uncaughtExceptionHandler = it }
        }
    }
    
    /**
     * Create a thread factory which calls the given factory function every time it is asked to create a thread.
     *
     * @param factory The factory function. The first argument is the sequence number of the thread being created, starting at 0.
     */
    fun multi(factory: (seq: Long, Runnable) -> Thread): ThreadFactory =
        object : ThreadFactory {
            private val counter = AtomicLong()
            override fun newThread(r: Runnable): Thread =
                factory(counter.getAndIncrement(), r)
        }
    
    /**
     * Create a thread factory which returns new thread with the given properties every time it is used.
     * The name, if any, is suffixed with a counter starting at 1.
     */
    fun multi(
        name: String? = null,
        daemon: Boolean? = null,
        priority: Int? = null,
        uncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null,
    ): ThreadFactory = multi { seq, r ->
        Thread(r).apply {
            name?.let { this.name = "${it}-${seq + 1}" }
            daemon?.let { this.isDaemon = it }
            priority?.let { this.priority = it }
            uncaughtExceptionHandler?.let { this.uncaughtExceptionHandler = it }
        }
    }
    
}

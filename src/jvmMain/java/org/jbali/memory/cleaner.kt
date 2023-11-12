package org.jbali.memory

import org.jbali.threads.ThreadFactoryFactory
import java.lang.ref.Cleaner
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A shared [Cleaner] instance for global use.
 * Starts a single daemon thread named "org.jbali.memory.globalCleaner-processor" that processes the phantom references,
 * and numbered "org.jbali.memory.globalCleaner-X" threads for executing the cleanup actions.
 */
val globalCleaner = Cleaner.create(ThreadFactoryFactory.multi { seq, r ->
    Thread(r, "org.jbali.memory.globalCleaner-${if (seq == 0L) "processor" else seq}")
})


interface Closer {
    fun close()
}


/**
 * Register a close action with this cleaner.
 *
 * @param obj The object to monitor
 * @param closer The close action to run. This will be invoked at most once. The `cleaning` parameter will be true if
 *               the action is being invoked by the cleaner, false if manually.
 * @return An [AutoCloseable] which will manually invoke the close action when closed.
 */
fun Cleaner.registerCloser(obj: Any, closer: (cleaning: Boolean) -> Unit): AutoCloseable {
    val co = object : AutoCloseable, Runnable {
        private val closed = AtomicBoolean(false)
        override fun close() {
            if (closed.compareAndSet(false, true)) {
                closer(false)
            }
        }
        override fun run() {
            if (closed.compareAndSet(false, true)) {
                closer(true)
            }
        }
    }
    register(obj, co)
    return co
}

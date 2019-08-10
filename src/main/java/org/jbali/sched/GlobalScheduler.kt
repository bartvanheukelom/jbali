package org.jbali.sched

import org.jbali.threads.ThreadPool
import org.jbali.threads.runWithThreadName
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Future
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

class StackRecording(m: String) : RuntimeException(m)

object GlobalScheduler : Scheduler() {

    // TODO LoggerDelegate
    private val log = LoggerFactory.getLogger(GlobalScheduler::class.java)

    private val exLock = ReentrantLock()
    private var shutDownStack: StackRecording? = null
    private var ex: ScheduledThreadPoolExecutor? = null

    val inited get() = exLock.withLock { ex != null }

    override val currentTime: Instant get() = Instant.now()

    private fun ex(): ScheduledThreadPoolExecutor =
        exLock.withLock {
            if (shutDownStack != null) throw IllegalStateException("GlobalScheduler was shut down", shutDownStack)
            if (ex == null) {
                // reading CPU cores is a safety net, but actually 1 thread should be enough
                // because no long-running tasks should run in the executor
                ex = ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors()).also {
                    it.setKeepAliveTime(30, TimeUnit.SECONDS)
                }
            }
            ex!!
        }

    fun shutdownNow(): Boolean {
        exLock.withLock {
            if (shutDownStack != null) throw IllegalStateException("GlobalScheduler was shut down", shutDownStack)
            val cex = ex
            cex?.shutdownNow()
            ex = null
            shutDownStack = StackRecording("Shutdown happened at ${Instant.now()} with stack")
            return cex != null
        }
    }

    /**
     * For testing
     */
    fun resetShutdownState() {
        exLock.withLock {
            check(ex == null)
            shutDownStack = null
        }
    }

    override fun scheduleReal(t: TaskToSchedule) =
            object : ScheduledTask {

                // please ensure t is not captured by not referring to it in any code that does not run at init time

                private val name = t.name
                private var body: TaskBody? = t.body

                override var state = ScheduledTask.State.SCHEDULED
                    private set
                    get() = lock.withLock {
                        field
                    }
                private val lock = ReentrantLock()

                private val scheduledTime = System.currentTimeMillis() + t.delay.toMillis()

                private var runningTask: Future<*>? = null
                private val firer = ex().schedule(::runNowInOtherThread, t.delay.toMillis(), TimeUnit.MILLISECONDS)

                override val currentDelay: Duration?
                    get() = lock.withLock {
                        when (state) {
                            ScheduledTask.State.SCHEDULED -> Duration.ofMillis(max(0, scheduledTime - System.currentTimeMillis()))
                            ScheduledTask.State.RUNNING,
                            ScheduledTask.State.COMPLETED,
                            ScheduledTask.State.ERRORED -> Duration.ZERO
                            ScheduledTask.State.CANCELLED -> null
                        }
                    }

                private fun runNowInOtherThread() {
                    runWithThreadName("GS.fire[$name]") {
                        lock.withLock {
                            // TODO allow specifying an external lock that should be synced too
                            if (state != ScheduledTask.State.SCHEDULED) {
                                assert(state == ScheduledTask.State.CANCELLED)
                            } else {
                                assert(runningTask == null)
                                runningTask = ThreadPool.submit(::runInCurrentThread)
                            }
                        }
                    }
                }

                private fun runInCurrentThread() {
                    runWithThreadName("GS.runr[$name]") {
                        val shouldRun =
                                lock.withLock {
                                    if (state != ScheduledTask.State.SCHEDULED) {
                                        assert(state == ScheduledTask.State.CANCELLED)
                                        false
                                    } else {
                                        state = ScheduledTask.State.RUNNING
                                        true
                                    }
                                }

                        if (shouldRun) {

                            val ns = try {
                                body!!()
                                ScheduledTask.State.COMPLETED
                            } catch (e: Throwable) {
                                log.error("Uncaught error in scheduled task $name", e)
                                ScheduledTask.State.ERRORED
                            } finally {
                                // release memory for body that is never run again
                                body = null
                            }

                            lock.withLock {
                                assert(state == ScheduledTask.State.RUNNING)
                                state = ns
                            }

                        }
                    }
                }

                // TODO do something with allowWhileRunning
                override fun cancel(interrupt: Boolean, allowWhileRunning: Boolean) =
                    lock.withLock {
                        val cancelled = when (state) {
                            ScheduledTask.State.SCHEDULED,
                            ScheduledTask.State.RUNNING -> firer.cancel(false) || (runningTask?.cancel(interrupt) ?: false)

                            ScheduledTask.State.COMPLETED,
                            ScheduledTask.State.ERRORED,
                            ScheduledTask.State.CANCELLED -> false
                        }
                        if (cancelled) state = ScheduledTask.State.CANCELLED

                        // release memory for body that won't ever run
                        body = null

                        cancelled
                    }

            }

}
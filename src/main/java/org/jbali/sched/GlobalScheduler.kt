package org.jbali.sched

import org.jbali.threads.ThreadPool
import org.jbali.threads.withThreadName
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.Future
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object GlobalScheduler : Scheduler() {

    // TODO LoggerDelegate
    private val log = LoggerFactory.getLogger(GlobalScheduler::class.java)

    private val exLock = ReentrantLock()
    private var shutDownStack: Throwable? = null
    private var ex: ScheduledThreadPoolExecutor? = null

    val inited get() = exLock.withLock { ex != null }

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
            shutDownStack = RuntimeException("Shutdown happened at ${Instant.now()} with stack")
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

                override var state = ScheduledTask.State.SCHEDULED
                    private set
                private val lock = ReentrantLock()

                private var runningTask: Future<*>? = null
                private val firer = ex().schedule(::runNowInOtherThread, t.delay.toMillis(), TimeUnit.MILLISECONDS)

                private fun runNowInOtherThread() {
                    withThreadName("GS.fire[${t.name}]") {
                        lock.withLock {
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
                    withThreadName("GS.runr[${t.name}]") {
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
                                t.body()
                                ScheduledTask.State.COMPLETED
                            } catch (e: Throwable) {
                                log.error("Uncaught error in scheduled task ${t.name}", e)
                                ScheduledTask.State.ERRORED
                            }

                            lock.withLock {
                                assert(state == ScheduledTask.State.RUNNING)
                                state = ns
                            }

                        }
                    }
                }

                override fun cancel(interrupt: Boolean) =
                    lock.withLock {
                        val cancelled = when (state) {
                            ScheduledTask.State.SCHEDULED,
                            ScheduledTask.State.RUNNING -> firer.cancel(false) || (runningTask?.cancel(interrupt) ?: false)

                            ScheduledTask.State.COMPLETED,
                            ScheduledTask.State.ERRORED,
                            ScheduledTask.State.CANCELLED -> false
                        }
                        if (cancelled) state = ScheduledTask.State.CANCELLED
                        cancelled
                    }

            }

}
package org.jbali.sched

import java.time.Duration

interface ScheduledTask {
    val state: State
    fun cancel(interrupt: Boolean = false): Boolean

    enum class State(
            /** True for completed, errored and cancelled */
            val willNotRun: Boolean
    ) {
        SCHEDULED(false), RUNNING(false), COMPLETED(true), ERRORED(true), CANCELLED(true)
    }
}

abstract class Scheduler {

    abstract fun schedule(t: TaskToSchedule): ScheduledTask

    inner class After(
            val delay: Duration
    ) {
        fun run(name: String = "<unnamed>", body: () -> Unit): ScheduledTask =
                schedule(TaskToSchedule(delay, body, name))
    }

    data class TaskToSchedule(
            val delay: Duration,
            val body: () -> Unit,
            val name: String
    )

    fun after(delay: Duration) = After(delay)


    // ------------------------------ after utils -------------------------------

    fun afterSeconds(s: Int) = After(Duration.ofSeconds(s.toLong()))
    fun afterSeconds(s: Long) = After(Duration.ofSeconds(s))

    fun afterMs(ms: Int) = After(Duration.ofMillis(ms.toLong()))
    fun afterMs(ms: Long) = After(Duration.ofMillis(ms))

    val asap = After(Duration.ofMillis(1))


    // ------------------------------ derived -------------------------------

    fun withTaskMapper(map: (TaskToSchedule) -> TaskToSchedule): Scheduler {
        val be = this
        return object : Scheduler() {
            override fun schedule(t: TaskToSchedule): ScheduledTask =
                    be.schedule(map(t))
        }
    }

    fun prefix(p: String) = withTaskMapper { it.copy(name = "$p: ${it.name}") }
    fun multiplied(m: Double) = withTaskMapper { it.copy(delay = it.delay * m) }

}

operator fun Duration.times(m: Double) = Duration.ofMillis((toMillis() * m).toLong())

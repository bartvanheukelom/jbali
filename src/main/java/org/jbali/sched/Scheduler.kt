package org.jbali.sched

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

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

typealias TaskBody = () -> Unit

abstract class Scheduler {

    private val tasks: MutableSet<ScheduledTask> = ConcurrentHashMap.newKeySet()

    protected abstract fun scheduleReal(t: TaskToSchedule): ScheduledTask

    private fun taskMaintenance() {
        tasks.removeIf { t -> t.state.willNotRun }
    }

    private fun tasksWith(f: (ScheduledTask) -> Boolean): Set<ScheduledTask> {
        taskMaintenance()
        return tasks.asSequence().filter(f).toSet()
    }

    fun schedule(t: TaskToSchedule) =
            scheduleReal(t).also {
                tasks.add(it)
                taskMaintenance()
            }

    val knownTasks get() = tasksWith { true }
    val scheduledTasks get() = tasksWith { it.state == ScheduledTask.State.SCHEDULED }
    val uncompletedTasks get() = tasksWith { !it.state.willNotRun }

    inner class After(
            val delay: Duration
    ) {
        fun run(name: String = "<unnamed>", body: TaskBody): ScheduledTask =
                schedule(TaskToSchedule(delay, body, name))
    }

    data class TaskToSchedule(
            val delay: Duration,
            val body: TaskBody,
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
            override fun scheduleReal(t: TaskToSchedule) =
                    be.schedule(map(t))
        }
    }

    fun prefix(p: String) = withTaskMapper { it.copy(name = "$p: ${it.name}") }
    fun multiplied(m: Double) = withTaskMapper { it.copy(delay = it.delay * m) }

    fun withTaskDecorator(deco: (target: TaskBody) -> TaskBody) =
            withTaskMapper { t -> t.copy(body = deco(t.body)) }

}

operator fun Duration.times(m: Double) = Duration.ofMillis((toMillis() * m).toLong())

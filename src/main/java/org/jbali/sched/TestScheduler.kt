package org.jbali.sched

import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import kotlin.math.max

class TestScheduler(name: String) : Scheduler() {

    private var seqqer = 0
    private val log = LoggerFactory.getLogger("${TestScheduler::class.java}[$name]")

    inner class PlannedTask(
            val task: TaskToSchedule,
            val time: Long
    ) : ScheduledTask, Comparable<PlannedTask> {

        val stack = RuntimeException()
        val seq = seqqer++

        val timeStr = run {
            val d = Duration.ofMillis(time)
            val min = d.toMinutes()
            val sec = d.seconds - (min * 60)
            val ms = d.nano / 1_000_000L
            String.format("#${seq.toString().padStart(5)}  T+%03d:%02d:%04d (%s)", min, sec, ms, (time - currentTime).toString().padStart(6))
        }

        override val currentDelay: Duration?
            get() = when (state) {
                ScheduledTask.State.SCHEDULED -> Duration.ofMillis(max(0, time - currentTime))
                ScheduledTask.State.RUNNING,
                ScheduledTask.State.COMPLETED,
                ScheduledTask.State.ERRORED -> Duration.ZERO
                ScheduledTask.State.CANCELLED -> null
            }

        override fun toString() = "$timeStr = ${task.name}"

        override fun compareTo(other: PlannedTask) =
                if (time == other.time) seq.compareTo(other.seq)
                else time.compareTo(other.time)

        override var state = ScheduledTask.State.SCHEDULED
            private set

        fun run() {
            check(state == ScheduledTask.State.SCHEDULED)
            state = ScheduledTask.State.RUNNING
            try {
                task.body()
                state = ScheduledTask.State.COMPLETED
            } catch (e: Throwable) {
                state = ScheduledTask.State.ERRORED
                log.warn("${task.name} was scheduled at:", stack)
                throw AssertionError("Error in TestScheduler task ${task.name}: $e", e)
            }
        }

        override fun cancel(interrupt: Boolean) =
                when (state) {
                    ScheduledTask.State.SCHEDULED -> {
                        state = ScheduledTask.State.CANCELLED
                        tasks.remove(this)
                        true
                    }

                    ScheduledTask.State.RUNNING -> throw AssertionError("Cancel while running? Are you using TestScheduler from multiple threads?")

                    ScheduledTask.State.COMPLETED,
                    ScheduledTask.State.ERRORED,
                    ScheduledTask.State.CANCELLED -> false
                }
    }

    var currentTime = 0L
        private set
    private val tasks = TreeSet<PlannedTask>()
    val queueEmpty get() = tasks.isEmpty()

    override fun scheduleReal(t: TaskToSchedule): ScheduledTask {
        val st = PlannedTask(t, currentTime + t.delay.toMillis())
        log.info("Schedule $st")
        tasks.add(st)
        return st
    }

    fun step(): Boolean {
        val t = tasks.pollFirst()
        return if (t != null) {
            currentTime += t.task.delay.toMillis()

            //QQQQ
            Thread.currentThread().name = "|${Duration.ofMillis(currentTime).toString()}|"

            log.info("""
                |
                |   Step: $t
                |""".trimMargin())
            t.run()
            true
        } else {
            false
        }
    }

    fun logQueue() {
        log.info("\n\tCurrent queue:\n" +
            tasks.joinToString(separator = "\n") {
                "\t\t$it"
            })
    }

}
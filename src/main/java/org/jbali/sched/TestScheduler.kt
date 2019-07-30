package org.jbali.sched

import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class TestScheduler(
        name: String,
        /** When running tasks, set the thread name to the current simulated time.
         * A hacky but easy way to get current time printed in logs. */
        val putTimeInThreadName: Boolean = false,
        val actualSleepFactor: Double = 0.0
) : Scheduler() {

    private var seqqer = 0
    private val log = LoggerFactory.getLogger("${TestScheduler::class.java.name}[$name]")

    fun formatTime(time: Long): String {
        val d = Duration.ofMillis(time)
        val min = d.toMinutes()
        val sec = d.seconds - (min * 60)
        val ms = d.nano / 1_000_000L
        return String.format("T+%03d:%02d:%04d", min, sec, ms)
    }

    inner class PlannedTask(
            val task: TaskToSchedule,
            val time: Long
    ) : ScheduledTask, Comparable<PlannedTask> {

        val stack = RuntimeException()
        val seq = seqqer++

        private val timeStr get() = "#${seq.toString().padStart(5)}  ${formatTime(time)} (-${(time - currentTimeLocal).toString().padStart(6)})"

        override val currentDelay: Duration?
            get() = when (state) {
                ScheduledTask.State.SCHEDULED -> Duration.ofMillis(max(0, time - currentTimeLocal))
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
                        log.info("-Cancel  $this")
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

    /** Simulated time in ms */
    var currentTimeLocal = 0L
        private set

    override val currentTime: Instant get() = Instant.ofEpochMilli(TimeUnit.DAYS.toMillis(1) + currentTimeLocal)

    private val tasks = TreeSet<PlannedTask>()
    val queueEmpty get() = tasks.isEmpty()

    override fun scheduleReal(t: TaskToSchedule): ScheduledTask {

        var targetTime = currentTimeLocal + t.delay.toMillis()
        if (t.roundToSecond) targetTime = ceil(targetTime / 1000.0).toLong() * 1000

        val st = PlannedTask(t, targetTime)

        log.info("Schedule $st")
        tasks.add(st)

        return st
    }

    /**
     * Advance to, and run, the next scheduled task.
     * @return false if there are no more tasks in the queue.
     */
    fun step(): Boolean {
        // get the next upcoming task
        val t = tasks.pollFirst()
        return if (t == null) false else {

            // advance to that time in simulated 1-second intervals
            while (currentTimeLocal < t.time) {
                if (actualSleepFactor != 0.0) Thread.sleep((1000.0 * actualSleepFactor).toLong())

                val left = t.time - currentTimeLocal
                System.err.println("... ${formatTime(currentTimeLocal)} (-${left / 1000.0}) ...")

                currentTimeLocal += min(1000, left)
                if (currentTimeLocal != t.time) currentTimeLocal = floor(currentTimeLocal / 1000.0).toLong() * 1000
            }

            if (putTimeInThreadName)
                Thread.currentThread().name = "|${Duration.ofMillis(currentTimeLocal)}|"

            // GO!
            log.info("""
                |
                |   Step: $t
                |""".trimMargin())
            t.run()

            true
        }
    }

    fun logQueue() {
        log.info("\n\tCurrent queue:\n" +
            tasks.joinToString(separator = "\n") {
                "\t\t$it"
            })
    }

}
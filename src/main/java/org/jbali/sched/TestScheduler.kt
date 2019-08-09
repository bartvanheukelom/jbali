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
import kotlin.random.Random

/**
 * Scheduler that can simulate time in a step-by-step fashion without any actual delays.
 * For this to work properly, tested code must always get the "current time" from the scheduler
 * and not from System.currentTime*, Instant.now or anything that uses the system clock.
 */
class TestScheduler(
        name: String,

        private val firstTime: Instant = Instant.ofEpochMilli(TimeUnit.DAYS.toMillis(1)),

        /** When running tasks, set the thread name to the current simulated time.
         * A hacky but easy way to get current time printed in logs. */
        private val putTimeInThreadName: Boolean = false,

        private val actualSleepFactor: Double = 0.0,

        /**
         * Will log the schedule-time stack when a scheduled task has an error.
         * However, just putting a breakpoint on TaskBodyException is preferred.
         * */
        private val logScheduleStack: Boolean = false
) : Scheduler() {

    class TaskBodyException(m: String, e: Throwable) : RuntimeException(m, e)

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
            task: TaskToSchedule,
            val time: Long
    ) : ScheduledTask, Comparable<PlannedTask> {

        val seq = seqqer++

        private val name = task.name
        private var body: TaskBody? = task.body

        // these are eagerly converted to readable text to help with debugger inspection
        private val schedTime = formatTime(currentTimeLocal)
        private val stack = Thread.currentThread().stackTrace.joinToString("\n")

        private val timeStr get() = "#${seq.toString().padStart(5)}  ${formatTime(time)} (-${(time - currentTimeLocal).toString().padStart(6)})"

        override val currentDelay: Duration?
            get() = when (state) {
                ScheduledTask.State.SCHEDULED -> Duration.ofMillis(max(0, time - currentTimeLocal))
                ScheduledTask.State.RUNNING,
                ScheduledTask.State.COMPLETED,
                ScheduledTask.State.ERRORED -> Duration.ZERO
                ScheduledTask.State.CANCELLED -> null
            }

        override fun toString() = "$timeStr = $name"

        override fun compareTo(other: PlannedTask) =
                if (time == other.time) seq.compareTo(other.seq)
                else time.compareTo(other.time)

        override var state = ScheduledTask.State.SCHEDULED
            private set

        fun run() {
            check(state == ScheduledTask.State.SCHEDULED)
            state = ScheduledTask.State.RUNNING
            try {
                body!!()
                state = ScheduledTask.State.COMPLETED
            } catch (e: Throwable) {
                state = ScheduledTask.State.ERRORED
                val ref: String
                if (logScheduleStack) {
                    ref = "[ref ${Random.nextInt(10000, 100000)}] "
                    log.warn("${ref}Task was scheduled from:\n${stack.prependIndent()}")
                } else {
                    ref = ""
                }
                throw TaskBodyException("${ref}Error in task $name that was scheduled at $schedTime: $e", e)
            } finally {
                // release memory for body that is never run again
                body = null
            }
        }

        override fun cancel(interrupt: Boolean) =
                when (state) {
                    ScheduledTask.State.SCHEDULED -> {
                        log.info("-Cancel  $this")
                        state = ScheduledTask.State.CANCELLED
                        tasks.remove(this)

                        // release memory for body that won't ever run
                        body = null

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

    override val currentTime: Instant get() = firstTime.plusMillis(currentTimeLocal)

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
     * @throws TaskBodyException if the next task throws an exception.
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
                Thread.currentThread().name = formatTime(currentTimeLocal)

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
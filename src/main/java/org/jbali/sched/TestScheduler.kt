package org.jbali.sched

import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class TestScheduler(name: String) : Scheduler() {

    var seqqer = 0
    val log = LoggerFactory.getLogger("${TestScheduler::class.java}[$name]")

    inner class PT(
            val task: TaskToSchedule,
            val time: Long
    ) : ScheduledTask, Comparable<PT> {

        val seq = seqqer++

        val timeStr = run {
            val d = Duration.ofMillis(time)
            val min = d.toMinutes()
            val sec = d.seconds - (min * 60)
            val ms = d.nano / 1_000_000L
            String.format("T+%03d:%02d:%04d (%s)", min, sec, ms, (time - currentTime).toString().padStart(6))
        }

        override fun toString() = "$timeStr = ${task.name}"

        override fun compareTo(other: PT) =
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
    private val tasks = TreeSet<PT>()

    override fun schedule(t: TaskToSchedule): ScheduledTask {
        val st = PT(t, currentTime + t.delay.toMillis())
        log.info("Schedule $st")
        tasks.add(st)
        return st
    }

    fun step(): Boolean {
        val t = tasks.pollFirst()
        return if (t != null) {
            currentTime += t.task.delay.toMillis()
            log.info("""Step:
                |  ${t.timeStr} =
                |   ${t.task.name}
                |""".trimMargin())
            t.run()
            true
        } else {
            false
        }
    }

    fun logQueue() {
        log.info("Current queue:\n" +
            tasks.joinToString(separator = "\n") {
                "\t$it"
            })
    }

}
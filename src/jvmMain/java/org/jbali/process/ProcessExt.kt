@file:OptIn(ExperimentalTime::class)

package org.jbali.process

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

fun buildProcess(vararg command: String): ProcessBuilder =
    ProcessBuilder(*command)
        .redirectInput(ProcessBuilder.Redirect.INHERIT)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)

@Throws(InterruptedException::class)
fun runProcess(vararg command: String) {
    buildProcess(*command)
        .run()
}

@Throws(InterruptedException::class)
fun runAndReadLines(vararg command: String): List<String> {
    val p = buildProcess(*command)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .start()
    
    val lines =
        p.inputStream.use { ins ->
            ins.reader().readLines()
        }
    
    // should complete instantly
    p.waitForSuccess(command.first().substringAfterLast('/'))
    return lines
}

/**
 * Start the process and wait for it to terminate successfully, or throw an exception.
 *
 * If the wait is stopped before the process terminates, the process will be destroyed.
 *
 * @throws RuntimeException if the process terminated with a non-0 exit value.
 * @throws TimeoutException if the process did not terminate before the given [timeout].
 * @throws InterruptedException if this thread is interrupted while waiting.
 */
@Throws(InterruptedException::class)
fun ProcessBuilder.run(
    name: String? = null,
    timeout: Duration? = null,
) {
    start().waitForSuccess(name = name, timeout = timeout)
}

typealias ProcessExitValue = Int

class ProcessExitValueException(msg: String, val exitValue: ProcessExitValue) : RuntimeException(msg)

/**
 * Wait for this process to terminate successfully, or throw an exception.
 *
 * If the wait is stopped before the process terminates, the process will be destroyed.
 *
 * @throws ProcessExitValueException if the process terminated with a non-0 exit value.
 * @throws TimeoutException if the process did not terminate before the given [timeout].
 * @throws InterruptedException if this thread is interrupted while waiting.
 */
@Throws(InterruptedException::class)
fun Process.waitForSuccess(
    name: String? = null,
    timeout: Duration? = null,
) {
    val exitValue = waitForExit(name = name, timeout = timeout)
    if (exitValue != 0) {
        throw ProcessExitValueException("Process${name?.let { " '$name'" } ?: ""} exited with value $exitValue", exitValue)
    }
}

/**
 * Wait for this process to terminate and return the [Process.exitValue].
 *
 * If the wait is stopped before the process terminates, the process will be destroyed.
 *
 * @throws TimeoutException if the process did not terminate before the given [timeout].
 * @throws InterruptedException if this thread is interrupted while waiting.
 */
@Throws(InterruptedException::class)
fun Process.waitForExit(
    name: String? = null,
    timeout: Duration? = null,
): ProcessExitValue {
    try {
        if (timeout != null) {
            if (!waitFor(timeout.toLongMilliseconds(), TimeUnit.MILLISECONDS)) {
                throw TimeoutException("Timeout while waiting for process ${name?.let { " '$name'" } ?: ""} to terminate")
            }
        } else {
            waitFor()
        }
        return exitValue()
    } catch (e: Throwable) {
        try {
            destroy()
        } catch (de: Throwable) {
            e.addSuppressed(de)
        }
        throw e
    }
}

// overloads that don't use ExperimentalTime
fun ProcessBuilder.run    (name: String? = null) = run           (name = name, timeout = null)
fun Process.waitForSuccess(name: String? = null) = waitForSuccess(name = name, timeout = null)
fun Process.waitForExit   (name: String? = null) = waitForExit   (name = name, timeout = null)

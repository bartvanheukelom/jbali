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
fun ProcessBuilder.run(timeout: Duration? = null) {
    start().waitForSuccess(timeout)
}

/**
 * Wait for this process to terminate successfully, or throw an exception.
 *
 * If the wait is stopped before the process terminates, the process will be destroyed.
 *
 * @throws RuntimeException if the process terminated with a non-0 exit value.
 * @throws TimeoutException if the process did not terminate before the given [timeout].
 * @throws InterruptedException if this thread is interrupted while waiting.
 */
@Throws(InterruptedException::class)
fun Process.waitForSuccess(timeout: Duration? = null) {
    
    try {
        if (timeout != null) {
            if (!waitFor(timeout.toLongMilliseconds(), TimeUnit.MILLISECONDS)) {
                throw TimeoutException("Timeout while waiting for process to terminate")
            }
        } else {
            waitFor()
        }
    } catch (e: Throwable) {
        try {
            destroy()
        } catch (de: Throwable) {
            e.addSuppressed(de)
        }
        throw e
    }
    
    if (exitValue() != 0) {
        throw RuntimeException("Process exited with value ${exitValue()}")
    }
}

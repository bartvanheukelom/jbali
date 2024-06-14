package org.jbali.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.lang.Long.min
import java.time.Duration
import java.time.Instant

@PublishedApi
internal val retryLog = LoggerFactory.getLogger("retry")

class GivenUpException(msg: String, cause: Throwable) : RuntimeException(msg, cause)

/**
 * Retry operation until it succeeds, throw a non-retryable error or exceeds the max attempts.
 * errorCallback will receive each exception thrown and should return whether another attempt should be made.
 * If a non-retryable exception is thrown, throws that exception.
 * If max attempts are exceed, throws [GivenUpException] with the last retryable exception as cause.
 * Blocks the thread while sleeping between attempts.
 */
inline fun <T> retry(
    initialSleep: Long = 250,
    maxSleep: Long = 5000,
    maxAttempts: Int? = null,
    maxDuration: Duration? = null,
    errorCallback: (e: Exception, attempt: Int) -> Boolean = { e, i -> retryLog.warn("Error in retry attempt #$i", e); true },
    operation: () -> T
): T {
    var attempts = 0
    var sleepTime = initialSleep
    var startTime = Instant.now()

//    val errors: MutableList<Exception> = mutableListOf()
    
    while (true) {
        attempts++
        try {
            return operation()
        } catch (e: Exception) {
            val retryable = errorCallback(e, attempts)
            if (!retryable) throw e
            val timeTaken = Duration.between(startTime, Instant.now())
            if (
                (maxAttempts != null && attempts >= maxAttempts) ||
                (maxDuration != null && timeTaken > maxDuration)
            ) {
                throw GivenUpException("Operation failed after $attempts attempts in $timeTaken. Last attempt: $e", e)
//                        .apply {
                // TODO this seemed like a good idea but it results in several screens of log, what do?
//                    errors.forEach(::addSuppressed)
//                }
            
            } else {

//                errors.add(e)
                
                if (initialSleep > 0) {
                    Thread.sleep(sleepTime)
                    sleepTime = min(sleepTime * 2, maxSleep)
                }
            }
        }
    }
}

/**
 * Retry operation until it succeeds, throw a non-retryable error or exceeds the max attempts.
 * errorCallback will receive each exception thrown and should return whether another attempt should be made.
 * If a non-retryable exception is thrown, throws that exception.
 * If max attempts are exceeded or the coroutine is cancelled, throws [GivenUpException] with the last retryable exception as cause.
 * Suspends while sleeping between attempts.
 */
suspend fun <T> retrySuspending(
    initialSleep: Long = 250,
    maxSleep: Long = 5000,
    maxAttempts: Int? = null,
    maxDuration: Duration? = null,
    errorCallback: (e: Exception, attempt: Int) -> Boolean = { e, i -> retryLog.warn("Error in retry attempt #$i", e); true },
    operation: suspend (attempt: Int) -> T
): T {
    var attempts = 0
    var sleepTime = initialSleep
    val startTime = NanoTime.now()
    
    val maxAtt = maxAttempts ?: Int.MAX_VALUE
    val maxDur = maxDuration?.toNanoDuration() ?: NanoDuration.MAX
    
    while (true) {
        try {
            return operation(attempts)
        } catch (e: Exception) {
            val retryable = errorCallback(e, attempts)
            if (!retryable) throw e
            
            attempts++
            val timeTaken = NanoDuration.since(startTime)
            if (attempts >= maxAtt || timeTaken > maxDur) {
                throw GivenUpException("Operation failed after $attempts attempts in $timeTaken. Last attempt: $e", e)
            } else {
                if (initialSleep > 0) {
                    try {
                        delay(sleepTime)
                    } catch (ce: CancellationException) {
                        throw GivenUpException("Operation cancelled after $attempts attempts in $timeTaken. Last attempt: $e", e)
                    }
                    sleepTime = (sleepTime * 2).coerceAtMost(maxSleep)
                }
            }
        }
    }
}

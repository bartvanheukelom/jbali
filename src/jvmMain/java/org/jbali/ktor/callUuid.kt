package org.jbali.ktor

import com.google.common.cache.CacheBuilder
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.micrometer.core.instrument.Metrics
import org.jbali.enums.EnumCompanion
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Install [CallId] in such a way that it tries to retrieve from the `X-Request-ID` header,
 * replies with the same header, and only accepts / generates [UUID]'s.
 *
 * Maintains a cache of recently _accepted_ UUID's to prevent reuse. Requests that attempt to reuse are always rejected.
 * [seenRetention] controls how long a UUID is kept in the cache.
 *
 * Malformed IDs in requests are ignored unless [rejectMalformed] is true, in which case they are rejected.
 * Use the latter in development to catch bugs early.
 */
fun ApplicationCallPipeline.installCallUuid(
    rejectMalformed: Boolean = false,
    seenRetention: Duration = Duration.ofMinutes(15),
) {
    
    val counters = CallIdResolution.associate {
        Metrics.counter("jbali.ktor.callid", "resolution", it.name.lowercase())
    }
    fun countResolution(resolution: CallIdResolution) {
        counters.getValue(resolution).increment()
    }
    
    val seenUuids = CacheBuilder.newBuilder()
        .expireAfterWrite(seenRetention.toMillis(), TimeUnit.MILLISECONDS)
        .build<String, Boolean>()
    
    fun checkUnique(cid: String) {
        synchronized(seenUuids) {
            if (seenUuids.getIfPresent(cid) != null) {
                countResolution(CallIdResolution.ReuseRejected)
                throw RejectedCallIdException("Request ID $cid already used")
            }
            seenUuids.put(cid, true)
        }
    }
    
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        replyToHeader(HttpHeaders.XRequestId)
        
        generate {
            UUID.randomUUID().toString()
                .also { checkUnique(it) } // no need to check, but this stores it
        }
        verify { cid ->
            when (uuidFromStringOrNull(cid)) {
                // malformed
                null -> when {
                    rejectMalformed -> {
                        countResolution(CallIdResolution.MalformedRejected)
                        throw BadRequestException("Request ID not a valid UUID: '$cid'")
                    }
                    else -> {
                        countResolution(CallIdResolution.MalformedIgnored)
                        false
                    }
                }
                // good
                else -> {
                    checkUnique(cid)
                    countResolution(CallIdResolution.Accepted)
                    true
                }
            }
        }
    }
}

enum class CallIdResolution {
    Accepted,
    Missing,
    MalformedIgnored,
    MalformedRejected,
    ReuseRejected;
    companion object : EnumCompanion<CallIdResolution>(CallIdResolution::class)
}

fun uuidFromStringOrNull(s: String): UUID? =
    try {
        UUID.fromString(s)
    } catch (iae: IllegalArgumentException) {
        null
    }

/**
 * The call's UUID as provided by the `X-Request-ID` header, or generated locally
 * if not provided.
 *
 * Must have been installed with [installCallUuid].
 */
val ApplicationCall.uuid: UUID get() =
    UUID.fromString(callId!!)

/**
 * The call's UUID as provided by the `X-Request-ID` header, or generated locally
 * if not provided.
 *
 * `null` if feature not installed with [installCallUuid] or a non-UUID version installed.
 */
val ApplicationCall.uuidOrNull: UUID? get() =
    try { callId?.let(UUID::fromString) }
    catch (e: IllegalArgumentException) { null }

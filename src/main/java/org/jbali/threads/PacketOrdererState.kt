package org.jbali.threads

import kotlinx.serialization.Serializable
import org.jbali.kotser.std.InstantSerializer
import java.time.Instant

@Serializable
data class PacketOrdererState<P>(
    val lastSeq: Long,
    val waiting: List<P>,
    @Serializable(with = InstantSerializer::class)
    val waitingSince: Instant?
)

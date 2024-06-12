package org.jbali.threeten

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime



val defaultZoneId: ZoneId get() = ZoneId.systemDefault()

fun LocalDateTime.atDefault(): ZonedDateTime = atZone(defaultZoneId)
fun LocalDateTime.toDefaultInstant(): Instant = atDefault().toInstant()

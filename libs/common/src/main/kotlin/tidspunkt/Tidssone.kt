package no.nav.etterlatte.libs.common.tidspunkt

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

val norskTidssone: ZoneId = ZoneId.of("Europe/Oslo")

val standardTidssoneUTC = ZoneOffset.UTC

fun LocalDateTime.tilInstant() = toInstant(standardTidssoneUTC)

fun Instant.tilZonedDateTime() = atZone(standardTidssoneUTC)

fun Timestamp.tilZonedDateTime() = toLocalDateTime().atZone(standardTidssoneUTC)

fun LocalDateTime.tilUTCTimestamp() =
    Timestamp.from(this.atZone(standardTidssoneUTC).toTidspunkt().instant)

fun Timestamp.tilUTCLocalDateTime() = toTidspunkt().instant.atZone(standardTidssoneUTC).toLocalDateTime()
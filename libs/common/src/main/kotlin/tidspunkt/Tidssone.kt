package no.nav.etterlatte.libs.common.tidspunkt

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

internal val norskTidssone: ZoneId = ZoneId.of("Europe/Oslo")

internal val standardTidssoneUTC = ZoneOffset.UTC

fun LocalDateTime.tilInstant() = toInstant(standardTidssoneUTC)

fun Instant.tilZonedDateTime() = atZone(standardTidssoneUTC)

fun Timestamp.tilZonedDateTime() = toLocalDateTime().atZone(standardTidssoneUTC)

fun LocalDateTime.tilUTCTimestamp() =
    Timestamp.from(this.atZone(standardTidssoneUTC).toTidspunkt().instant)

fun Timestamp.tilUTCLocalDateTime() = toTidspunkt().instant.atZone(standardTidssoneUTC).toLocalDateTime()

fun Instant?.toLocalDateTimeNorskTid() = this?.let { LocalDateTime.ofInstant(it, norskTidssone) }

fun LocalDate.midnattNorskTid() = atStartOfDay(norskTidssone)
fun nowNorskTid(): ZonedDateTime = ZonedDateTime.now(norskTidssone)
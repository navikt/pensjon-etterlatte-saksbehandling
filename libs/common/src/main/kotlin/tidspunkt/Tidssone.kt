package no.nav.etterlatte.libs.common.tidspunkt

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

val norskTidssone: ZoneId = ZoneId.of("Europe/Oslo")

val standardTidssone = ZoneOffset.UTC

fun LocalDateTime.tilInstant() = toInstant(standardTidssone)

fun Instant.tilZonedDateTime() = atZone(standardTidssone)

fun Timestamp.tilZonedDateTime() = toLocalDateTime().atZone(standardTidssone)

fun LocalDateTime.tilSystemDefaultTimestamp() =
    Timestamp.from(this.atZone(ZoneId.systemDefault()).toTidspunkt().instant)

fun Timestamp.tilSystemDefaultLocalDateTime() = toTidspunkt().instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
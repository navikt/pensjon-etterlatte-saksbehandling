package no.nav.etterlatte.libs.common.tidspunkt

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/*
Denne fila samlar extension functions for tidskonvertering vi forhåpentlegvis ikkje treng på sikt,
sia målet er å gå over til å bruke Tidspunkt ganske konsekvent
 */

fun Instant.tilZonedDateTime() = atZone(standardTidssoneUTC)

fun Timestamp.tilZonedDateTime() = toLocalDateTime().atZone(standardTidssoneUTC)

fun LocalDateTime.tilUTCTimestamp() =
    Timestamp.from(this.atZone(standardTidssoneUTC).toTidspunkt().instant)

fun Timestamp.tilUTCLocalDateTime() = toTidspunkt().instant.atZone(standardTidssoneUTC).toLocalDateTime()

fun Instant?.toLocalDateTimeNorskTid() = this?.let { LocalDateTime.ofInstant(it, norskTidssone) }

fun LocalDate.midnattNorskTid() = atStartOfDay(norskTidssone)
fun nowNorskTid(): ZonedDateTime = ZonedDateTime.now(norskTidssone)

fun Instant.toTidspunkt() = Tidspunkt(this)
fun ZonedDateTime.toTidspunkt() = toInstant().toTidspunkt()
fun LocalDateTime.toTidspunkt(zoneId: ZoneId = standardTidssoneUTC) = atZone(zoneId).toTidspunkt()
fun LocalDateTime.toNorskTidspunkt() = toTidspunkt(zoneId = norskTidssone)
fun Tidspunkt.toNorskTid(): ZonedDateTime = ZonedDateTime.ofInstant(this.instant, norskTidssone)
fun Tidspunkt.toTimestamp(): Timestamp = Timestamp.from(this.instant)
fun Timestamp.toTidspunkt(): Tidspunkt = Tidspunkt(this.toInstant())
fun Tidspunkt.toLocalDatetimeUTC(): LocalDateTime = LocalDateTime.ofInstant(this.instant, standardTidssoneUTC)
fun Tidspunkt.toLocalDatetimeNorskTid(): LocalDateTime = LocalDateTime.ofInstant(this.instant, norskTidssone)
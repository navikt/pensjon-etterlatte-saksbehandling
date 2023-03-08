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

fun Timestamp.tilUTCLocalDateTime(): LocalDateTime = toTidspunkt().instant.atZone(standardTidssoneUTC).toLocalDateTime()

fun Instant?.toLocalDateTimeNorskTid() = this?.let { LocalDateTime.ofInstant(it, norskTidssone) }

fun LocalDate.midnattNorskTid(): ZonedDateTime = atStartOfDay(norskTidssone)

fun ZonedDateTime.toTidspunkt() = Tidspunkt(this.toInstant())
fun LocalDateTime.toTidspunkt(zoneId: ZoneId = standardTidssoneUTC) = atZone(zoneId).toTidspunkt()
fun LocalDateTime.toNorskTidspunkt() = toTidspunkt(zoneId = norskTidssone)
fun Tidspunkt.toNorskTid(): ZonedDateTime = ZonedDateTime.ofInstant(this.instant, norskTidssone)
fun Tidspunkt.toLocalDatetimeUTC(): LocalDateTime = LocalDateTime.ofInstant(this.instant, standardTidssoneUTC)
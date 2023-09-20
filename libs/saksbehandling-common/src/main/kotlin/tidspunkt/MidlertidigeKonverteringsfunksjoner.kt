package no.nav.etterlatte.libs.common.tidspunkt

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/*
Denne fila samlar extension functions for tidskonvertering vi forhåpentlegvis ikkje treng på sikt,
sia målet er å gå over til å bruke Tidspunkt ganske konsekvent
 */

fun LocalDateTime.toTidspunkt(zoneId: ZoneId = standardTidssoneUTC) = Tidspunkt(atZone(zoneId).toInstant())

fun LocalDateTime.toNorskTidspunkt() = toTidspunkt(zoneId = norskTidssone)

fun Tidspunkt.toNorskTid(): ZonedDateTime = ZonedDateTime.ofInstant(this.instant, norskTidssone)

fun Tidspunkt.toLocalDatetimeUTC(): LocalDateTime = LocalDateTime.ofInstant(this.instant, standardTidssoneUTC)

fun Tidspunkt.toLocalDatetimeNorskTid(): LocalDateTime = LocalDateTime.ofInstant(this.instant, norskTidssone)

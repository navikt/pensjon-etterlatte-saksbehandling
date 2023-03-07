package no.nav.etterlatte.libs.common.tidspunkt

import java.time.Clock
import java.time.ZonedDateTime

fun klokke(): Clock = Clock.systemUTC()

fun Clock.norskKlokke(): Clock = withZone(norskTidssone)

fun ZonedDateTime.fixedNorskTid() = toTidspunkt().fixedNorskTid()

fun Tidspunkt.fixedNorskTid() = Clock.fixed(this.instant, norskTidssone)
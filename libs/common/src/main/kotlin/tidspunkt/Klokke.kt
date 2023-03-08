package no.nav.etterlatte.libs.common.tidspunkt

import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime

fun klokke(): Clock = Clock.systemUTC()

fun Clock.norskKlokke(): Clock = withZone(norskTidssone)

fun Instant.fixedNorskTid(): Clock = Clock.fixed(this, norskTidssone)

fun ZonedDateTime.fixedNorskTid() = toInstant().fixedNorskTid()
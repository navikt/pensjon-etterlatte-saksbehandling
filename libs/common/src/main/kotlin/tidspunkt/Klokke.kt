package no.nav.etterlatte.libs.common.tidspunkt

import java.time.Clock
import java.time.Instant

fun klokke() = Clock.systemUTC()

fun norskKlokke(clock: Clock) = clock.withZone(norskTidssone)

fun fixedNorskTid(instant: Instant) = Clock.fixed(instant, norskTidssone)
package no.nav.etterlatte.libs.common.tidspunkt

import java.time.Clock

fun utcKlokke(): Clock = Clock.systemUTC()

fun norskKlokke(): Clock = utcKlokke().norskKlokke()

fun Clock.norskKlokke(): Clock = withZone(norskTidssone)

fun Tidspunkt.fixedNorskTid(): Clock = Clock.fixed(this.instant, norskTidssone)

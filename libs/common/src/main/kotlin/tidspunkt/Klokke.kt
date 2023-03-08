package no.nav.etterlatte.libs.common.tidspunkt

import java.time.Clock

fun klokke(): Clock = Clock.systemUTC()

fun Clock.norskKlokke(): Clock = withZone(norskTidssone)

fun Tidspunkt.fixedNorskTid(): Clock = Clock.fixed(this.instant, norskTidssone)
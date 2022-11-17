package no.nav.etterlatte.statistikk.utils

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.sql.Timestamp

fun Tidspunkt.toTimestamp(): Timestamp {
    return Timestamp.from(this.instant)
}

fun Timestamp.toTidspunkt(): Tidspunkt {
    return Tidspunkt(this.toInstant())
}
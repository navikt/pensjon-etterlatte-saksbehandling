package no.nav.etterlatte.libs.database

import kotliquery.Row
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt

fun Row.tidspunkt(columnLabel: String) = sqlTimestamp(columnLabel).toTidspunkt()
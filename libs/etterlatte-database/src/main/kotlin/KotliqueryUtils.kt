package no.nav.etterlatte.libs.database

import kotliquery.Row

fun Row.timeStamp(columnLabel: String) = sqlTimestamp(columnLabel)
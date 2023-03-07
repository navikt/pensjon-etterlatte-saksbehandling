package no.nav.etterlatte.libs.common.tidspunkt

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp

fun Tidspunkt.toTimestamp(): Timestamp = Timestamp.from(this.instant)
fun Timestamp.toTidspunkt(): Tidspunkt = Tidspunkt(this.toInstant())
fun PreparedStatement.setTidspunkt(index: Int, value: Tidspunkt?) = setTimestamp(index, value?.toTimestamp())
fun ResultSet.getTidspunkt(name: String) = getTimestamp(name)?.toInstant()?.let { Tidspunkt(it) }
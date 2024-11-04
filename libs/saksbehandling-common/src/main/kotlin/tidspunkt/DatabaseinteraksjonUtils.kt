package no.nav.etterlatte.libs.common.tidspunkt

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.util.UUID

fun Tidspunkt.toTimestamp(): Timestamp = Timestamp.from(this.instant)

fun Timestamp.toTidspunkt(): Tidspunkt = Tidspunkt(this.toInstant())

fun PreparedStatement.setTidspunkt(
    index: Int,
    value: Tidspunkt?,
) = setTimestamp(index, value?.toTimestamp())

fun ResultSet.getTidspunkt(name: String): Tidspunkt =
    getTidspunktOrNull(name) ?: throw IllegalStateException("Forventa at tidspunkt for $name ikke er null")

fun ResultSet.getTidspunktOrNull(name: String): Tidspunkt? = getTimestamp(name)?.toInstant()?.let { Tidspunkt(it) }

fun ResultSet.getLongOrNull(name: String): Long? =
    if (getObject(name) == null) {
        null
    } else {
        getLong(name)
    }

fun PreparedStatement.setLongOrNull(
    index: Int,
    value: Long?,
) = if (value == null) setNull(index, Types.BIGINT) else setLong(index, value)

fun ResultSet.getUUID(name: String) = getObject(name) as UUID

fun PreparedStatement.setIntOrNull(
    index: Int,
    value: Int?,
) = if (value == null) setNull(index, Types.BIGINT) else setInt(index, value)

fun ResultSet.getIntOrNull(name: String): Int? =
    if (getObject(name) == null) {
        null
    } else {
        getInt(name)
    }

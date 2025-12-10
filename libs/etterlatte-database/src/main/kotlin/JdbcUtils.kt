package no.nav.etterlatte.libs.database

import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import org.postgresql.util.PGobject
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDate

fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? =
    if (next()) {
        block().also {
            krev(!next()) { "Skal være unik" }
        }
    } else {
        null
    }

fun <T> ResultSet.single(block: ResultSet.() -> T): T {
    krev(next()) {
        "Skal ha en verdi"
    }
    return block().also {
        krev(!next()) { "Skal være unik" }
    }
}

fun <T> ResultSet.firstOrNull(block: ResultSet.() -> T): T? =
    if (next()) {
        block()
    } else {
        null
    }

fun <T> ResultSet.toList(block: ResultSet.() -> T): List<T> {
    val list = ArrayList<T>()
    while (next()) {
        list.add(block())
    }
    return list
}

inline fun <reified T : Any> PreparedStatement.setJsonb(
    parameterIndex: Int,
    jsonb: T?,
): PreparedStatement {
    if (jsonb == null) {
        this.setNull(parameterIndex, java.sql.Types.NULL)
    }
    val jsonObject = PGobject()
    jsonObject.type = "json"
    jsonObject.value = objectMapper.writeValueAsString(jsonb)
    this.setObject(parameterIndex, jsonObject)
    return this
}

fun PreparedStatement.setSakId(
    index: Int,
    sakId: SakId,
) = this.setLong(index, sakId.sakId)

fun PreparedStatement.setNullableInt(
    index: Int,
    value: Int?,
) = when (value) {
    null -> this.setNull(index, Types.BIGINT)
    else -> this.setInt(index, value)
}

fun PreparedStatement.setNullableDate(
    index: Int,
    value: LocalDate?,
) = when (value) {
    null -> this.setNull(index, Types.DATE)
    else -> this.setDate(index, Date.valueOf(value))
}

fun PreparedStatement.setNullableLong(
    index: Int,
    value: Long?,
) = when (value) {
    null -> this.setNull(index, Types.BIGINT)
    else -> this.setLong(index, value)
}

fun PreparedStatement.setNullableDouble(
    index: Int,
    value: Double?,
) = when (value) {
    null -> this.setNull(index, Types.FLOAT)
    else -> this.setDouble(index, value)
}

fun PreparedStatement.setNullableBoolean(
    index: Int,
    value: Boolean?,
) = when (value) {
    null -> this.setNull(index, Types.BOOLEAN)
    else -> this.setBoolean(index, value)
}

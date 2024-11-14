package no.nav.etterlatte.libs.database

import no.nav.etterlatte.libs.common.feilhaandtering.checkInternFeil
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.SakId
import org.postgresql.util.PGobject
import java.sql.PreparedStatement
import java.sql.ResultSet

fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? =
    if (next()) {
        block().also {
            checkInternFeil(!next()) { "Skal være unik" }
        }
    } else {
        null
    }

fun <T> ResultSet.single(block: ResultSet.() -> T): T {
    checkInternFeil(next()) {
        "Skal ha en verdi"
    }
    return block().also {
        checkInternFeil(!next()) { "Skal være unik" }
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

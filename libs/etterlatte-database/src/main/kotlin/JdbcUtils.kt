package no.nav.etterlatte.libs.database

import no.nav.etterlatte.libs.common.objectMapper
import org.postgresql.util.PGobject
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet

fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? =
    if (next()) {
        block().also {
            require(!next()) { "Skal være unik" }
        }
    } else {
        null
    }

fun <T> ResultSet.single(block: ResultSet.() -> T): T {
    require(next()) {
        "Skal ha en verdi"
    }
    return block().also {
        require(!next()) { "Skal være unik" }
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

fun oppdater(
    statement: String,
    params: List<SQLParameter<Any>>,
    connection: ConnectionAutoclosing,
) {
    connection.hentConnection {
        with(it) {
            val stmt = prepareStatement(statement.trimMargin())
            params.forEach { param -> settParameter(param, stmt) }
            stmt.executeUpdate()
        }
    }
}

private fun <T> settParameter(
    param: SQLParameter<T>,
    stmt: PreparedStatement,
) {
    when (param.type) {
        String::class.java -> stmt.setString(param.index, param.verdi as String?)
        Int::class.java -> stmt.setInt(param.index, param.verdi as Int)
        Long::class.java -> stmt.setLong(param.index, param.verdi as Long)
        Date::class.java -> stmt.setDate(param.index, param.verdi as Date?)
        else -> stmt.setObject(param.index, param.verdi)
    }
}

data class SQLParameter<T>(
    val index: Int,
    val type: Class<T>,
    val verdi: Any,
)

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

fun ConnectionAutoclosing.oppdater(
    statement: String,
    params: List<SQLParameter>,
) = hentConnection {
    with(it) {
        val stmt = prepareStatement(statement.trimMargin())
        params.forEachIndexed { index, param -> settParameter(index, param, stmt) }
        stmt.executeUpdate()
    }
}

private fun settParameter(
    index: Int,
    param: SQLParameter,
    stmt: PreparedStatement,
) = when (param.type) {
    Parametertype.STRING -> stmt.setString(index, param.verdi as String?)
    Parametertype.INT -> stmt.setInt(index, param.verdi as Int)
    Parametertype.LONG -> stmt.setLong(index, param.verdi as Long)
    Parametertype.DATE -> stmt.setDate(index, param.verdi as Date?)
    else -> stmt.setObject(index, param.verdi)
}

data class SQLParameter(
    val type: Parametertype,
    val verdi: Any?,
)

enum class Parametertype {
    STRING,
    INT,
    LONG,
    DATE,
    OBJECT,
}
